package org.smartboot.mqtt.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AckMessage;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttConnectPayload;
import org.smartboot.mqtt.common.message.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.WillMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.util.QuickTimerTask;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MqttClient extends AbstractSession implements Closeable {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final MqttClientConfigure clientConfigure = new MqttClientConfigure();
    private final AbstractMessageProcessor<MqttMessage> messageProcessor = new MqttClientProcessor(this);
    /**
     * 完成connect之前注册的事件
     */
    private final ConcurrentLinkedQueue<Runnable> registeredTasks = new ConcurrentLinkedQueue<>();
    /**
     * 已订阅的消息主题
     */
    private final Map<String, Subscribe> subscribes = new ConcurrentHashMap<>();
    private AioQuickClient client;

    private AsynchronousChannelGroup asynchronousChannelGroup;
    private BufferPagePool bufferPagePool;
    private boolean connected = false;

    /**
     * connect ack 回调
     */
    private Consumer<MqttConnAckMessage> consumer;

    public MqttClient(String host, int port, String clientId) {
        super(new QosPublisher());
        clientConfigure.setHost(host);
        clientConfigure.setPort(port);
        this.clientId = clientId;
    }

    public void connect() {
        try {
            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                private AtomicInteger index = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "ClientGroup" + index.getAndIncrement());
                }
            });
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        connect(asynchronousChannelGroup, connAckMessage -> {
        });
    }

    public void connect(AsynchronousChannelGroup asynchronousChannelGroup, Consumer<MqttConnAckMessage> consumer) {
        if (bufferPagePool == null) {
            bufferPagePool = new BufferPagePool(1024 * 1024 * 2, 10, true);
        }
        //设置 connect ack 回调事件
        this.consumer = mqttConnAckMessage -> {
            if (!clientConfigure.isAutomaticReconnect()) {
                gcConfigure();
            }

            //连接成功,注册订阅消息
            if (mqttConnAckMessage.getMqttConnAckVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                connected = true;
                Runnable runnable;
                while ((runnable = registeredTasks.poll()) != null) {
                    runnable.run();
                }
            }
            //客户端设置清理会话（CleanSession）标志为 0 重连时，客户端和服务端必须使用原始的报文标识符重发
            //任何未确认的 PUBLISH 报文（如果 QoS>0）和 PUBREL 报文 [MQTT-4.4.0-1]。这是唯一要求客户端或
            //服务端重发消息的情况。
            if (!clientConfigure.isCleanSession()) {
                responseConsumers.values().forEach(ackMessage -> write(ackMessage.getOriginalMessage()));
            }
            consumer.accept(mqttConnAckMessage);
        };
        //启动心跳插件
        if (clientConfigure.getKeepAliveInterval() > 0) {
            QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
                @Override
                public void run() {
                    if (session.isInvalid()) {
                        if (clientConfigure.isAutomaticReconnect()) {
                            LOGGER.warn("mqtt client is disconnect, try to reconnect...");
                            connect(asynchronousChannelGroup, consumer);
                        }
                        return;
                    }
                    long delay = System.currentTimeMillis() - getLatestSendMessageTime() - clientConfigure.getKeepAliveInterval() * 1000L;
                    //gap 10ms
                    if (delay > -10) {
                        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
                        MqttPingReqMessage pingReqMessage = new MqttPingReqMessage(mqttFixedHeader);
                        write(pingReqMessage);
                        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, clientConfigure.getKeepAliveInterval(), TimeUnit.SECONDS);
                    } else {
                        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, -delay, TimeUnit.MILLISECONDS);
                    }
                }
            }, clientConfigure.getKeepAliveInterval(), TimeUnit.SECONDS);
        }
//        messageProcessor.addPlugin(new StreamMonitorPlugin<>());
        client = new AioQuickClient(clientConfigure.getHost(), clientConfigure.getPort(), new MqttProtocol(), messageProcessor);
        try {
            client.setBufferPagePool(bufferPagePool);
            client.setWriteBuffer(1024 * 1024, 10);
            session = client.start(asynchronousChannelGroup);
            //remainingLength 字段动态计算，此处可传入任意值
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(clientConfigure.getMqttVersion(), StringUtils.isNotBlank(clientConfigure.getUserName()), clientConfigure.getPassword() != null, clientConfigure.getWillMessage(), clientConfigure.isCleanSession(), clientConfigure.getKeepAliveInterval());
            String willTopic = null;
            byte[] willMessage = null;
            if (clientConfigure.getWillMessage() != null) {
                willTopic = clientConfigure.getWillMessage().getWillTopic();
                willMessage = clientConfigure.getWillMessage().getWillMessage();
            }
            MqttConnectPayload payload = new MqttConnectPayload(clientId, willTopic, willMessage, clientConfigure.getUserName(), clientConfigure.getPassword());
            MqttConnectMessage connectMessage = new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
            write(connectMessage);
            connected = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 释放本地内存
     */
    private void gcConfigure() {
        clientConfigure.setWillMessage(null);
    }


    public MqttClient subscribe(String topic, MqttQoS qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        return subscribe(new String[]{topic}, new MqttQoS[]{qos}, consumer);
    }

    public MqttClient subscribe(String[] topics, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        if (connected) {
            subscribe0(topics, qos, consumer);
        } else {
            registeredTasks.offer(() -> subscribe0(topics, qos, consumer));
        }
        return this;
    }

    public void subscribe0(String[] topic, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        MqttMessageBuilders.SubscribeBuilder subscribeBuilder = MqttMessageBuilders.subscribe().packetId(newPacketId());
        for (int i = 0; i < topic.length; i++) {
            subscribeBuilder.addSubscription(qos[i], topic[i]);
        }
        MqttSubscribeMessage subscribeMessage = subscribeBuilder.build();
        responseConsumers.put(subscribeMessage.getPacketId(), new AckMessage(subscribeMessage, mqttMessage -> {
            List<Integer> qosValues = ((MqttSubAckMessage) mqttMessage).getMqttSubAckPayload().grantedQoSLevels();
            ValidateUtils.isTrue(qosValues.size() == qos.length, "invalid response");
            int i = 0;
            for (MqttTopicSubscription subscription : subscribeMessage.getMqttSubscribePayload().topicSubscriptions()) {
                MqttQoS minQos = MqttQoS.valueOf(Math.min(subscription.qualityOfService().value(), qosValues.get(i++)));
                clientConfigure.getTopicListener().subscribe(subscription.topicFilter(), subscription.qualityOfService() == MqttQoS.FAILURE ? MqttQoS.FAILURE : minQos);
                if (subscription.qualityOfService() != MqttQoS.FAILURE) {
                    subscribes.put(subscription.topicFilter(), new Subscribe(subscription.topicFilter(), minQos, consumer));
                } else {
                    LOGGER.error("subscribe topic:{} fail", subscription.topicFilter());
                }
            }
        }));
        write(subscribeMessage);
    }

    public void notifyResponse(MqttConnAckMessage connAckMessage) {
        consumer.accept(connAckMessage);
    }


    /**
     * 设置遗嘱消息，必须在connect之前调用
     */
    public MqttClient willMessage(WillMessage willMessage) {
        clientConfigure.setWillMessage(willMessage);
        return this;
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain, Consumer<Integer> consumer) {
        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().topicName(topic).qos(qos).payload(payload).retained(retain);
        if (qos.value() > 0) {
            int packetId = newPacketId();
            publishBuilder.packetId(packetId);
        }
        MqttPublishMessage message = publishBuilder.build();
        if (connected) {
            publish(message, consumer);
        } else {
            registeredTasks.offer(() -> publish(message, consumer));
        }
    }

    public MqttClientConfigure getClientConfigure() {
        return clientConfigure;
    }

    public Map<String, Subscribe> getSubscribes() {
        return subscribes;
    }

    @Override
    public void close() {
        //关闭自动重连
        clientConfigure.setAutomaticReconnect(false);
        client.shutdown();
    }

}
