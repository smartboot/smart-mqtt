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
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.DefaultMqttWriter;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.MqttProtocol;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttDisConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import org.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import org.smartboot.mqtt.common.message.variable.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttDisconnectVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.message.variable.properties.DisConnectProperties;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MqttClient extends AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClient.class);
    private static final Consumer<Integer> IGNORE = integer -> {
    };
    private static final HashedWheelTimer TIMER = new HashedWheelTimer(r -> new Thread(r, "client-timer"), 50, 1024);
    /**
     * 客户端配置项
     */
    private final Options options = new Options();
    private static final AbstractMessageProcessor<MqttMessage> messageProcessor = new MqttClientProcessor();

    /**
     * 完成connect之前注册的事件
     */
    private final ConcurrentLinkedQueue<Runnable> registeredTasks = new ConcurrentLinkedQueue<>();
    /**
     * 已订阅的消息主题
     */
    private final Map<String, Subscribe> subscribes = new ConcurrentHashMap<>();

    private final Map<String, List<Subscribe>> mapping = new ConcurrentHashMap<>();

    private final List<TopicToken> wildcardsToken = new LinkedList<>();

    private AioQuickClient client;


    private boolean connected = false;

    /**
     * connect ack 回调
     */
    private Consumer<MqttConnAckMessage> connectConsumer;

    int pingTimeout;

    private TimerTask connectTimer;

    public MqttClient(String uri) {
        this(uri, opt -> {
        });
    }

    public MqttClient(String host, int port) {
        this(host, port, opt -> {
        });
    }

    public MqttClient(String host, int port, Consumer<Options> opt) {
        this("mqtt://" + host + ":" + port, opt);
    }

    public MqttClient(String uri, Consumer<Options> opt) {
        super(TIMER);
        String[] array = uri.split(":");
        if (array[0].equals("mqtts")) {
            options.setHost(array[1].substring(2));
            //加密通信
        } else if (array[0].equals("mqtt")) {
            options.setHost(array[1].substring(2));
        } else {
            throw new IllegalStateException("invalid URI Scheme, uri: " + uri);
        }
        options.setPort(NumberUtils.toInt(array[2]));
        opt.accept(options);
        this.clientId = options.getClientId();
    }


    public void connect() {
        connect(connAckMessage -> {
        });
    }


    public void connect(Consumer<MqttConnAckMessage> consumer) {
        //设置 connect ack 回调事件
        this.connectConsumer = consumer;
        MqttUtil.updateConfig(options, "mqtt.client");
//        LOGGER.info("mqtt client config:{}", clientConfigure);
//        messageProcessor.addPlugin(new StreamMonitorPlugin<>());

        client = new AioQuickClient(options.getHost(), options.getPort(), new MqttProtocol(options.getMaxPacketSize()), messageProcessor);
        try {
            client.setReadBufferSize(options.getBufferSize()).setWriteBuffer(options.getBufferSize(), 8).connectTimeout(options.getConnectionTimeout());
            if (options.group() != null) {
                session = client.start(options.group());
            } else {
                session = client.start();
            }
            session.setAttachment(this);
            mqttWriter = new DefaultMqttWriter(session.writeBuffer());

            //todo
            ConnectProperties properties = null;
            if (options.getMqttVersion() == MqttVersion.MQTT_5) {
                properties = new ConnectProperties();
            }
            MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(options.getMqttVersion(), StringUtils.isNotBlank(options.getUserName()), options.getPassword() != null, options.getWillMessage(), options.isCleanSession(), options.getKeepAliveInterval(), properties);
            MqttConnectPayload payload = new MqttConnectPayload(clientId, options.getWillMessage(), options.getUserName(), options.getPassword());

            MqttConnectMessage connectMessage = new MqttConnectMessage(variableHeader, payload);

            //如果客户端在合理的时间内没有收到服务端的 CONNACK 报文，客户端应该关闭网络连接。
            // 合理的时间取决于应用的类型和通信基础设施。
            connectTimer = timer.schedule(new AsyncTask() {
                @Override
                public void execute() {
                    if (!connected) {
                        session.close(true);
                    }
                }
            }, options.getConnectAckTimeout(), TimeUnit.SECONDS);
            write(connectMessage);
            //启动心跳插件
            long keepAliveInterval = TimeUnit.SECONDS.toMillis(options.getKeepAliveInterval());
            if (keepAliveInterval > 0) {
                timer.schedule(new AsyncTask() {
                    @Override
                    public void execute() {
                        //客户端发送了 PINGREQ 报文之后，如果在合理的时间内仍没有收到 PINGRESP 报文，
                        // 它应该关闭到服务端的网络连接。
                        if (pingTimeout >= 3) {
                            pingTimeout = 0;
                            session.close(true);
                            return;
                        }
                        long delay = System.currentTimeMillis() - latestSendMessageTime - keepAliveInterval;
                        //gap 10ms
                        if (delay > -10) {
                            MqttPingReqMessage pingReqMessage = new MqttPingReqMessage();
                            write(pingReqMessage);
                            pingTimeout++;
                            timer.schedule(this, keepAliveInterval, TimeUnit.MILLISECONDS);
                        } else {
//                        LOGGER.info("client:{} ping listening was triggered early {}ms", clientId, -delay);
                            timer.schedule(this, -delay, TimeUnit.MILLISECONDS);
                        }
                    }
                }, keepAliveInterval, TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            release();
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
        options.setWillMessage(null);
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
        if (options.getMqttVersion() == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            unsubscribeBuilder.properties(properties);
        }
        // wait ack message.
        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = getInflightQueue().offer(unsubscribeBuilder);
        if (future == null) {
            registeredTasks.offer(() -> unsubscribe0(topics));
            return;
        }
        future.whenComplete((message, throwable) -> {
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
        if (options.getMqttVersion() == MqttVersion.MQTT_5) {
            subscribeBuilder.subscribeProperties(new SubscribeProperties());
        }
        MqttSubscribeMessage subscribeMessage = subscribeBuilder.build();

        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = getInflightQueue().offer(subscribeBuilder);
        if (future == null) {
            registeredTasks.offer(() -> subscribe0(topic, qos, consumer, subAckConsumer));
            return;
        }
        future.whenComplete((message, throwable) -> {
            List<Integer> qosValues = ((MqttSubAckMessage) message).getPayload().grantedQoSLevels();
            ValidateUtils.isTrue(qosValues.size() == qos.length, "invalid response");
            int i = 0;
            for (MqttTopicSubscription subscription : subscribeMessage.getPayload().getTopicSubscriptions()) {
                MqttQoS minQos = MqttQoS.valueOf(Math.min(subscription.getQualityOfService().value(), qosValues.get(i++)));
                options.getTopicListener().subscribe(subscription.getTopicFilter(), subscription.getQualityOfService() == MqttQoS.FAILURE ? MqttQoS.FAILURE : minQos);
                if (subscription.getQualityOfService() != MqttQoS.FAILURE) {
                    subscribes.put(subscription.getTopicFilter(), new Subscribe(minQos, consumer));
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
        flush();

    }

    void receiveConnAckMessage(MqttConnAckMessage connAckMessage) {
        connectTimer.cancel();
        connectTimer = null;
        if (!options.isAutomaticReconnect()) {
            gcConfigure();
        }

        //连接成功,注册订阅消息
        if (connAckMessage.getVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
            setInflightQueue(new InflightQueue(this, options.getMaxInflight()));
            //重连情况下重新触发订阅逻辑
            subscribes.forEach((k, v) -> {
                subscribe(k, v.getQoS(), v.getConsumer());
            });
            connected = true;
            consumeTask();
        }
        //客户端设置清理会话（CleanSession）标志为 0 重连时，客户端和服务端必须使用原始的报文标识符重发
        //任何未确认的 PUBLISH 报文（如果 QoS>0）和 PUBREL 报文 [MQTT-4.4.0-1]。这是唯一要求客户端或
        //服务端重发消息的情况。
        if (!options.isCleanSession()) {
            //todo
        }
        connectConsumer.accept(connAckMessage);
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
        if (options.getMqttVersion() == MqttVersion.MQTT_5) {
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
        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = inflightQueue.put(publishBuilder);
        future.whenComplete((message, throwable) -> consumer.accept(message.getVariableHeader().getPacketId()));
        if (autoFlush) {
            flush();
        }
    }


    private List<TopicToken> getWildcardsToken() {
        return wildcardsToken;
    }

    @Override
    public void accepted(MqttPublishMessage mqttPublishMessage) {
        MqttPublishVariableHeader header = mqttPublishMessage.getVariableHeader();
        List<Subscribe> cacheSubscribes = mapping.get(header.getTopicName());
        if (cacheSubscribes == null) {
            cacheSubscribes = new LinkedList<>();
            Subscribe subscribe = subscribes.get(header.getTopicName());
            //尝试通配符匹配
            if (subscribe == null) {
                subscribe = matchWildcardsSubscribe(header.getTopicName());
            }
            if (subscribe != null) {
                cacheSubscribes.add(subscribe);
            }
            cacheSubscribes.addAll(matchShareSubscribe(header.getTopicName()));
            if (!cacheSubscribes.isEmpty()) {
                mapping.put(header.getTopicName(), cacheSubscribes);
            }
        }

        // If unsubscribed, maybe null.
        if (cacheSubscribes.isEmpty()) {
            return;
        }
        if (cacheSubscribes.size() == 1) {
            cacheSubscribes.get(0).getConsumer().accept(this, mqttPublishMessage);
        } else {
            Subscribe s = cacheSubscribes.remove(0);
            s.getConsumer().accept(this, mqttPublishMessage);
            cacheSubscribes.add(s);
        }
    }

    private List<Subscribe> matchShareSubscribe(String topicName) {
        List<Subscribe> matchedSubscribes = new ArrayList<>();

        // 遍历所有订阅关系
        for (Map.Entry<String, Subscribe> entry : subscribes.entrySet()) {
            String topicFilter = entry.getKey();
            // 识别共享订阅格式
            if (topicFilter.startsWith("$share/")) {
                // 解析共享订阅的组名和实际主题
                String[] parts = topicFilter.split("/", 3);
                if (parts.length < 3) {
                    continue; // 无效的共享订阅格式
                }
                String actualTopic = parts[2];

                // 创建主题令牌进行通配符匹配
                TopicToken inputToken = new TopicToken(topicName);
                TopicToken subscribeToken = new TopicToken(actualTopic);

                // 匹配实际主题且组名相同
                if (MqttUtil.match(inputToken, subscribeToken)) {
                    matchedSubscribes.add(entry.getValue());
                }
            }
        }
        return matchedSubscribes;
    }

    private Subscribe matchWildcardsSubscribe(String topicName) {
        TopicToken publicTopicToken = new TopicToken(topicName);
        TopicToken matchToken = getWildcardsToken().stream().filter(topicToken -> MqttUtil.match(publicTopicToken, topicToken)).findFirst().orElse(null);
        return matchToken != null ? subscribes.get(matchToken.getTopicFilter()) : null;
    }

    /**
     * 客户端发送 DISCONNECT 报文之后：
     * <li> 必须关闭网络连接 [MQTT-3.14.4-1]。</li>
     * <li>不能通过那个网络连接再发送任何控制报文 [MQTT-3.14.4-2]</li>
     */
    @Override
    public void disconnect() {
        if (disconnect) {
            return;
        }
        //DISCONNECT 报文是客户端发给服务端的最后一个控制报文。表示客户端正常断开连接。
        try {
            if (options.getMqttVersion() == MqttVersion.MQTT_5) {
                MqttDisconnectVariableHeader variableHeader = new MqttDisconnectVariableHeader(MqttDisConnectReturnCode.NORMAL_DISCONNECT, new DisConnectProperties());
                MqttDisconnectMessage message = new MqttDisconnectMessage(variableHeader);
                write(message);
            } else {
                write(new MqttDisconnectMessage());
            }

        } finally {
            //关闭自动重连
            options.setAutomaticReconnect(false);
            disconnect = true;
            release();
        }
    }

    void release() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
        if (options.isAutomaticReconnect()) {
            LOGGER.warn("mqtt client:{} is disconnect, try to reconnect...", clientId);
            TIMER.schedule(() -> connect(options.reconnectConsumer() == null ? connectConsumer : options.reconnectConsumer()), options.getMaxReconnectDelay(), TimeUnit.MILLISECONDS);
        }
    }

}
