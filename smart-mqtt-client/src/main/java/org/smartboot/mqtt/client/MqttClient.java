/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.DefaultMqttWriter;
import org.smartboot.mqtt.common.InflightMessage;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.QosRetryPlugin;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import org.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import org.smartboot.mqtt.common.message.payload.WillMessage;
import org.smartboot.mqtt.common.message.variable.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;
import org.smartboot.mqtt.common.message.variable.properties.WillProperties;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.util.Attachment;
import org.smartboot.socket.util.QuickTimerTask;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MqttClient extends AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClient.class);
    private static final Consumer<Integer> IGNORE = integer -> {
    };
    /**
     * 客户端配置项
     */
    private final MqttClientConfigure clientConfigure = new MqttClientConfigure();
    private static final AbstractMessageProcessor<MqttMessage> messageProcessor = new MqttClientProcessor();

    static {
        messageProcessor.addPlugin(new QosRetryPlugin());
    }

    /**
     * 完成connect之前注册的事件
     */
    private final ConcurrentLinkedQueue<Runnable> registeredTasks = new ConcurrentLinkedQueue<>();
    /**
     * 已订阅的消息主题
     */
    private final Map<String, Subscribe> subscribes = new ConcurrentHashMap<>();

    private final Map<String, Subscribe> mapping = new ConcurrentHashMap<>();

    private final List<TopicToken> wildcardsToken = new LinkedList<>();

    private AioQuickClient client;

    private AsynchronousChannelGroup asynchronousChannelGroup;
    private boolean connected = false;

    /**
     * connect ack 回调
     */
    private Consumer<MqttConnAckMessage> connectConsumer;

    /**
     * 重连Consumer
     */
    private Consumer<MqttConnAckMessage> reconnectConsumer;
    private boolean pingTimeout = false;

    public MqttClient(String host, int port, String clientId) {
        this(host, port, clientId, MqttVersion.MQTT_3_1_1);
    }

    public MqttClient(String host, int port, String clientId, MqttVersion mqttVersion) {
        super(new EventBusImpl(EventType.types()));
        clientConfigure.setHost(host);
        clientConfigure.setPort(port);
        clientConfigure.setMqttVersion(mqttVersion);
        this.clientId = clientId;
        //ping-pong消息超时监听
        getEventBus().subscribe(EventType.RECEIVE_MESSAGE, (eventType, object) -> {
            if (object.getObject() instanceof MqttPingRespMessage) {
                pingTimeout = false;
            }
        });
        getEventBus().subscribe(EventType.WRITE_MESSAGE, (eventType, object) -> {
            if (object.getObject() instanceof MqttPingReqMessage) {
                pingTimeout = true;
            }
        });
    }

    public void connect() {
        try {
            asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(2, new ThreadFactory() {
                private int i = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "mqtt-client-" + MqttClient.this.hashCode() + "-" + (i++));
                }
            });
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        connect(asynchronousChannelGroup);
    }

    public void connect(AsynchronousChannelGroup asynchronousChannelGroup) {
        connect(asynchronousChannelGroup, connAckMessage -> {
        });
    }


    public void connect(AsynchronousChannelGroup asynchronousChannelGroup, BufferPagePool bufferPagePool) {
        connect(asynchronousChannelGroup, bufferPagePool, connAckMessage -> {
        });
    }


    public void connect(AsynchronousChannelGroup asynchronousChannelGroup, Consumer<MqttConnAckMessage> consumer) {
        connect(asynchronousChannelGroup, null, consumer);
    }

    public void connect(AsynchronousChannelGroup asynchronousChannelGroup, BufferPagePool bufferPagePool, Consumer<MqttConnAckMessage> consumer) {
        //设置 connect ack 回调事件
        this.connectConsumer = mqttConnAckMessage -> {
            if (!clientConfigure.isAutomaticReconnect()) {
                gcConfigure();
            }

            //连接成功,注册订阅消息
            if (mqttConnAckMessage.getVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                setInflightQueue(new InflightQueue(this, 16));
                connected = true;
                //重连情况下重新触发订阅逻辑
                subscribes.forEach((k, v) -> {
                    subscribe(k, v.getQoS(), v.getConsumer());
                });
                consumeTask();
            }
            //客户端设置清理会话（CleanSession）标志为 0 重连时，客户端和服务端必须使用原始的报文标识符重发
            //任何未确认的 PUBLISH 报文（如果 QoS>0）和 PUBREL 报文 [MQTT-4.4.0-1]。这是唯一要求客户端或
            //服务端重发消息的情况。
            if (!clientConfigure.isCleanSession()) {
                //todo
            }
            consumer.accept(mqttConnAckMessage);
            connected = true;
        };
        //启动心跳插件
        if (clientConfigure.getKeepAliveInterval() > 0) {
            QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
                @Override
                public void run() {
                    //客户端发送了 PINGREQ 报文之后，如果在合理的时间内仍没有收到 PINGRESP 报文，
                    // 它应该关闭到服务端的网络连接。
                    if (pingTimeout) {
                        pingTimeout = false;
                        client.shutdown();
                    }
                    if (session.isInvalid()) {
                        if (clientConfigure.isAutomaticReconnect()) {
                            LOGGER.warn("mqtt client is disconnect, try to reconnect...");
                            connect(asynchronousChannelGroup, reconnectConsumer == null ? consumer : reconnectConsumer);
                        }
                        return;
                    }
                    long delay = System.currentTimeMillis() - getLatestSendMessageTime() - clientConfigure.getKeepAliveInterval() * 1000L;
                    //gap 10ms
                    if (delay > -10) {
                        MqttPingReqMessage pingReqMessage = new MqttPingReqMessage();
                        write(pingReqMessage);
                        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, clientConfigure.getKeepAliveInterval(), TimeUnit.SECONDS);
                    } else {
                        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, -delay, TimeUnit.MILLISECONDS);
                    }
                }
            }, clientConfigure.getKeepAliveInterval(), TimeUnit.SECONDS);
        }
//        messageProcessor.addPlugin(new StreamMonitorPlugin<>());

        client = new AioQuickClient(clientConfigure.getHost(), clientConfigure.getPort(), new MqttProtocol(clientConfigure.getMaxPacketSize()), messageProcessor);
        try {
            if (bufferPagePool != null) {
                client.setBufferPagePool(bufferPagePool);
            }
            client.setReadBufferSize(clientConfigure.getBufferSize()).setWriteBuffer(clientConfigure.getBufferSize(), 8).connectTimeout(clientConfigure.getConnectionTimeout());
            session = client.start(asynchronousChannelGroup);
            Attachment attachment = new Attachment();
            session.setAttachment(attachment);
            attachment.put(MqttClientProcessor.SESSION_KEY, this);
            setMqttVersion(clientConfigure.getMqttVersion());
            mqttWriter = new DefaultMqttWriter(session.writeBuffer());

            //todo
            ConnectProperties properties = null;
            if (clientConfigure.getMqttVersion() == MqttVersion.MQTT_5) {
                properties = new ConnectProperties();
            }
            MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(clientConfigure.getMqttVersion(), StringUtils.isNotBlank(clientConfigure.getUserName()), clientConfigure.getPassword() != null, clientConfigure.getWillMessage(), clientConfigure.isCleanSession(), clientConfigure.getKeepAliveInterval(), properties);
            MqttConnectPayload payload = new MqttConnectPayload(clientId, clientConfigure.getWillMessage(), clientConfigure.getUserName(), clientConfigure.getPassword());

            MqttConnectMessage connectMessage = new MqttConnectMessage(variableHeader, payload);

            //如果客户端在合理的时间内没有收到服务端的 CONNACK 报文，客户端应该关闭网络连接。
            // 合理的时间取决于应用的类型和通信基础设施。
            QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                if (!connected) {
                    disconnect();
                }
            }, clientConfigure.getConnectAckTimeout(), TimeUnit.SECONDS);
            write(connectMessage);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void consumeTask() {
        Runnable runnable = registeredTasks.poll();
        if (runnable != null) {
            runnable.run();
        }
    }

    /**
     * 释放本地内存
     */
    private void gcConfigure() {
        clientConfigure.setWillMessage(null);
    }

    public MqttClient unsubscribe(String topic) {
        return unsubscribe(new String[]{topic});
    }

    public MqttClient unsubscribe(String[] topics) {
        if (connected) {
            unsubscribe0(topics);
        } else {
            registeredTasks.offer(() -> unsubscribe0(topics));
        }
        return this;
    }

    private void unsubscribe0(String[] topics) {
        Set<String> unsubscribedTopics = new HashSet<>(topics.length);
        for (String unsubscribedTopic : topics) {
            if (subscribes.containsKey(unsubscribedTopic)) {
                unsubscribedTopics.add(unsubscribedTopic);
            }
        }

        if (unsubscribedTopics.isEmpty()) {
            LOGGER.warn("empty unsubscribe topics detected!");
            return;
        }

        MqttMessageBuilders.UnsubscribeBuilder unsubscribeBuilder = MqttMessageBuilders.unsubscribe();
        unsubscribedTopics.forEach(unsubscribeBuilder::addTopicFilter);

        //todo
        if (getMqttVersion() == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            unsubscribeBuilder.properties(properties);
        }
        // wait ack message.
        getInflightQueue().offer(unsubscribeBuilder, (message) -> {
            ValidateUtils.isTrue(message instanceof MqttUnsubAckMessage, "uncorrected message type.");
            for (String unsubscribedTopic : unsubscribedTopics) {
                subscribes.remove(unsubscribedTopic);
                wildcardsToken.removeIf(topicToken -> StringUtils.equals(unsubscribedTopic, topicToken.getTopicFilter()));
            }
            mapping.clear();
            consumeTask();
        });
        flush();
    }

    public MqttClient subscribe(String topic, MqttQoS qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        return subscribe(new String[]{topic}, new MqttQoS[]{qos}, consumer);
    }

    public MqttClient subscribe(String topic, MqttQoS qos, BiConsumer<MqttClient, MqttPublishMessage> consumer, BiConsumer<MqttClient, MqttQoS> subAckConsumer) {
        return subscribe(new String[]{topic}, new MqttQoS[]{qos}, consumer, subAckConsumer);
    }

    public MqttClient subscribe(String[] topics, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        subscribe(topics, qos, consumer, (mqttClient, mqttQoS) -> {
        });
        return this;
    }

    public MqttClient subscribe(String[] topics, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer, BiConsumer<MqttClient, MqttQoS> subAckConsumer) {
        if (connected) {
            subscribe0(topics, qos, consumer, subAckConsumer);
        } else {
            registeredTasks.offer(() -> subscribe0(topics, qos, consumer, subAckConsumer));
        }
        return this;
    }

    private void subscribe0(String[] topic, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer, BiConsumer<MqttClient, MqttQoS> subAckConsumer) {
        MqttMessageBuilders.SubscribeBuilder subscribeBuilder = MqttMessageBuilders.subscribe();
        for (int i = 0; i < topic.length; i++) {
            subscribeBuilder.addSubscription(qos[i], topic[i]);
        }
        //todo
        if (clientConfigure.getMqttVersion() == MqttVersion.MQTT_5) {
            subscribeBuilder.subscribeProperties(new SubscribeProperties());
        }
        MqttSubscribeMessage subscribeMessage = subscribeBuilder.build();
        InflightMessage inflightMessage = getInflightQueue().offer(subscribeBuilder, (message) -> {
            List<Integer> qosValues = ((MqttSubAckMessage) message).getPayload().grantedQoSLevels();
            ValidateUtils.isTrue(qosValues.size() == qos.length, "invalid response");
            int i = 0;
            for (MqttTopicSubscription subscription : subscribeMessage.getPayload().getTopicSubscriptions()) {
                MqttQoS minQos = MqttQoS.valueOf(Math.min(subscription.getQualityOfService().value(), qosValues.get(i++)));
                clientConfigure.getTopicListener().subscribe(subscription.getTopicFilter(), subscription.getQualityOfService() == MqttQoS.FAILURE ? MqttQoS.FAILURE : minQos);
                if (subscription.getQualityOfService() != MqttQoS.FAILURE) {
                    subscribes.put(subscription.getTopicFilter(), new Subscribe(subscription.getTopicFilter(), minQos, consumer));
                    //缓存统配匹配的topic
                    TopicToken topicToken = new TopicToken(subscription.getTopicFilter());
                    if (topicToken.isWildcards()) {
                        wildcardsToken.add(topicToken);
                    }
                } else {
                    LOGGER.error("subscribe topic:{} fail", subscription.getTopicFilter());
                }
                mapping.clear();
                subAckConsumer.accept(MqttClient.this, minQos);
            }
            consumeTask();
        });
        if (inflightMessage == null) {
            registeredTasks.offer(() -> subscribe0(topic, qos, consumer, subAckConsumer));
        } else {
            flush();
        }
    }

    public void notifyResponse(MqttConnAckMessage connAckMessage) {
        connectConsumer.accept(connAckMessage);
    }


    /**
     * 设置遗嘱消息，必须在connect之前调用
     */
    public MqttClient willMessage(WillMessage willMessage) {
        ValidateUtils.notNull(willMessage, "willMessage can't be null");
        if (clientConfigure.getMqttVersion() != MqttVersion.MQTT_5 && willMessage.getProperties() != null) {
            ValidateUtils.throwException("will properties only support on mqtt5");
        } else if (clientConfigure.getMqttVersion() == MqttVersion.MQTT_5 && willMessage.getProperties() == null) {
            willMessage.setProperties(new WillProperties());
        }
        clientConfigure.setWillMessage(willMessage);
        return this;
    }

    public void publish(String topic, MqttQoS qos, byte[] payload) {
        publish(topic, qos, payload, false, true);
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain) {
        publish(topic, qos, payload, retain, true);
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain, boolean autoFlush) {
        publish(topic, qos, payload, retain, IGNORE, autoFlush);
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, Consumer<Integer> consumer) {
        publish(topic, qos, payload, false, consumer, true);
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain, Consumer<Integer> consumer) {
        publish(topic, qos, payload, retain, consumer, true);
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain, Consumer<Integer> consumer, boolean autoFlush) {
        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().topicName(topic).qos(qos).payload(payload).retained(retain);
        //todo
        if (getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }
        if (connected) {
            publish(publishBuilder, consumer, autoFlush);
        } else {
            registeredTasks.offer(() -> publish(publishBuilder, consumer, autoFlush));
        }
    }

    private void publish(MqttMessageBuilders.PublishBuilder publishBuilder, Consumer<Integer> consumer, boolean autoFlush) {
        if (publishBuilder.qos() == MqttQoS.AT_MOST_ONCE) {
            write(publishBuilder.build(), autoFlush);
            consumer.accept(0);
            return;
        }
        InflightQueue inflightQueue = getInflightQueue();
        InflightMessage inflightMessage = inflightQueue.offer(publishBuilder, (message) -> {
            consumer.accept(message.getVariableHeader().getPacketId());
            //最早发送的消息若收到响应，则更新点位
            synchronized (MqttClient.this) {
                MqttClient.this.notifyAll();
            }
        });
        if (inflightMessage == null) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            publish(publishBuilder, consumer, autoFlush);
            return;
        }
        if (autoFlush) {
            flush();
        }
    }

    public MqttClientConfigure getClientConfigure() {
        return clientConfigure;
    }

    public Map<String, Subscribe> getSubscribes() {
        return subscribes;
    }

    public Map<String, Subscribe> getMapping() {
        return mapping;
    }

    public List<TopicToken> getWildcardsToken() {
        return wildcardsToken;
    }

    /**
     * 客户端发送 DISCONNECT 报文之后：
     * <li> 必须关闭网络连接 [MQTT-3.14.4-1]。</li>
     * <li>不能通过那个网络连接再发送任何控制报文 [MQTT-3.14.4-2]</li>
     */
    @Override
    public void disconnect() {
        //DISCONNECT 报文是客户端发给服务端的最后一个控制报文。表示客户端正常断开连接。
        write(new MqttDisconnectMessage());
        //关闭自动重连
        clientConfigure.setAutomaticReconnect(false);
        disconnect = true;
        client.shutdown();
    }

    public void setReconnectConsumer(Consumer<MqttConnAckMessage> reconnectConsumer) {
        this.reconnectConsumer = reconnectConsumer;
    }
}
