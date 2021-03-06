package org.smartboot.mqtt.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AckMessage;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.eventbus.EventBusImpl;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttConnectPayload;
import org.smartboot.mqtt.common.message.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.WillMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.util.QuickTimerTask;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MqttClient extends AbstractSession {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    /**
     * ??????????????????
     */
    private final MqttClientConfigure clientConfigure = new MqttClientConfigure();
    private final AbstractMessageProcessor<MqttMessage> messageProcessor = new MqttClientProcessor(this);
    /**
     * ??????connect?????????????????????
     */
    private final ConcurrentLinkedQueue<Runnable> registeredTasks = new ConcurrentLinkedQueue<>();
    /**
     * ????????????????????????
     */
    private final Map<String, Subscribe> subscribes = new ConcurrentHashMap<>();

    private AioQuickClient client;

    private AsynchronousChannelGroup asynchronousChannelGroup;
    private BufferPagePool bufferPagePool;
    private boolean connected = false;

    /**
     * connect ack ??????
     */
    private Consumer<MqttConnAckMessage> consumer;
    private boolean pingTimeout = false;

    public MqttClient(String host, int port, String clientId) {
        super(new ClientQosPublisher(), new EventBusImpl(EventType.types()));
        clientConfigure.setHost(host);
        clientConfigure.setPort(port);
        this.clientId = clientId;
        //ping-pong??????????????????
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
        connect(asynchronousChannelGroup);
    }

    public void connect(AsynchronousChannelGroup asynchronousChannelGroup) {
        connect(asynchronousChannelGroup, connAckMessage -> {
        });
    }

    public void connect(AsynchronousChannelGroup asynchronousChannelGroup, Consumer<MqttConnAckMessage> consumer) {
//        if (bufferPagePool == null) {
//            bufferPagePool = new BufferPagePool(1024 * 1024 * 2, 10, true);
//        }
        //?????? connect ack ????????????
        this.consumer = mqttConnAckMessage -> {
            if (!clientConfigure.isAutomaticReconnect()) {
                gcConfigure();
            }

            //????????????,??????????????????
            if (mqttConnAckMessage.getVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                connected = true;
                Runnable runnable;
                while ((runnable = registeredTasks.poll()) != null) {
                    runnable.run();
                }
            }
            //??????????????????????????????CleanSession???????????? 0 ???????????????????????????????????????????????????????????????????????????
            //?????????????????? PUBLISH ??????????????? QoS>0?????? PUBREL ?????? [MQTT-4.4.0-1]?????????????????????????????????
            //?????????????????????????????????
            if (!clientConfigure.isCleanSession()) {
                responseConsumers.values().forEach(ackMessage -> write(ackMessage.getOriginalMessage()));
            }
            consumer.accept(mqttConnAckMessage);
            connected = true;
        };
        //??????????????????
        if (clientConfigure.getKeepAliveInterval() > 0) {
            QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
                @Override
                public void run() {
                    //?????????????????? PINGREQ ????????????????????????????????????????????????????????? PINGRESP ?????????
                    // ?????????????????????????????????????????????
                    if (pingTimeout) {
                        pingTimeout = false;
                        client.shutdown();
                    }
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
        client = new AioQuickClient(clientConfigure.getHost(), clientConfigure.getPort(), new MqttProtocol(), messageProcessor);
        try {
//            client.setBufferPagePool(bufferPagePool);
            client.setWriteBuffer(1024 * 1024, 10);
            session = client.start(asynchronousChannelGroup);

            MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(clientConfigure.getMqttVersion(), StringUtils.isNotBlank(clientConfigure.getUserName()), clientConfigure.getPassword() != null, clientConfigure.getWillMessage(), clientConfigure.isCleanSession(), clientConfigure.getKeepAliveInterval());
            String willTopic = null;
            byte[] willMessage = null;
            if (clientConfigure.getWillMessage() != null) {
                willTopic = clientConfigure.getWillMessage().getWillTopic();
                willMessage = clientConfigure.getWillMessage().getWillMessage();
            }
            MqttConnectPayload payload = new MqttConnectPayload(clientId, willTopic, willMessage, clientConfigure.getUserName(), clientConfigure.getPassword());
            MqttConnectMessage connectMessage = new MqttConnectMessage(variableHeader, payload);

            //???????????????????????????????????????????????????????????? CONNACK ?????????????????????????????????????????????
            // ???????????????????????????????????????????????????????????????
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

    /**
     * ??????????????????
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

    public void unsubscribe0(String[] topics) {
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

        MqttMessageBuilders.UnsubscribeBuilder unsubscribeBuilder = MqttMessageBuilders.unsubscribe().packetId(newPacketId());
        unsubscribedTopics.forEach(unsubscribeBuilder::addTopicFilter);

        MqttUnsubscribeMessage unsubscribedMessage = unsubscribeBuilder.build();
        // wait ack message.
        responseConsumers.put(unsubscribedMessage.getVariableHeader().getPacketId(), new AckMessage(unsubscribedMessage, mqttMessage -> {
            ValidateUtils.isTrue(mqttMessage instanceof MqttUnsubAckMessage, "uncorrected message type.");
            for (String unsubscribedTopic : unsubscribedTopics) {
                subscribes.remove(unsubscribedTopic);
            }
        }));
        write(unsubscribedMessage);
    }

    public MqttClient subscribe(String topic, MqttQoS qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        return subscribe(new String[]{topic}, new MqttQoS[]{qos}, consumer);
    }

    public MqttClient subscribe(String topic, MqttQoS qos, BiConsumer<MqttClient, MqttPublishMessage> consumer, BiConsumer<MqttClient, MqttQoS> subAckConsumer) {
        return subscribe(new String[]{topic}, new MqttQoS[]{qos}, consumer, subAckConsumer);
    }

    public MqttClient subscribe(String[] topics, MqttQoS[] qos, BiConsumer<MqttClient, MqttPublishMessage> consumer) {
        subscribe0(topics, qos, consumer, (mqttClient, mqttQoS) -> {
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
        MqttMessageBuilders.SubscribeBuilder subscribeBuilder = MqttMessageBuilders.subscribe().packetId(newPacketId());
        for (int i = 0; i < topic.length; i++) {
            subscribeBuilder.addSubscription(qos[i], topic[i]);
        }
        MqttSubscribeMessage subscribeMessage = subscribeBuilder.build();
        responseConsumers.put(subscribeMessage.getVariableHeader().getPacketId(), new AckMessage(subscribeMessage, mqttMessage -> {
            List<Integer> qosValues = ((MqttSubAckMessage) mqttMessage).getMqttSubAckPayload().grantedQoSLevels();
            ValidateUtils.isTrue(qosValues.size() == qos.length, "invalid response");
            int i = 0;
            for (MqttTopicSubscription subscription : subscribeMessage.getMqttSubscribePayload().getTopicSubscriptions()) {
                MqttQoS minQos = MqttQoS.valueOf(Math.min(subscription.getQualityOfService().value(), qosValues.get(i++)));
                clientConfigure.getTopicListener().subscribe(subscription.getTopicFilter(), subscription.getQualityOfService() == MqttQoS.FAILURE ? MqttQoS.FAILURE : minQos);
                if (subscription.getQualityOfService() != MqttQoS.FAILURE) {
                    subscribes.put(subscription.getTopicFilter(), new Subscribe(subscription.getTopicFilter(), minQos, consumer));
                } else {
                    LOGGER.error("subscribe topic:{} fail", subscription.getTopicFilter());
                }
                subAckConsumer.accept(this, minQos);
            }
        }));
        write(subscribeMessage);
    }

    public void notifyResponse(MqttConnAckMessage connAckMessage) {
        consumer.accept(connAckMessage);
    }


    /**
     * ??????????????????????????????connect????????????
     */
    public MqttClient willMessage(WillMessage willMessage) {
        clientConfigure.setWillMessage(willMessage);
        return this;
    }

    public void publish(String topic, MqttQoS qos, byte[] payload, boolean retain) {
        publish(topic, qos, payload, retain, integer -> {

        });
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

    /**
     * ??????????????? DISCONNECT ???????????????
     * <li> ???????????????????????? [MQTT-3.14.4-1]???</li>
     * <li>????????????????????????????????????????????????????????? [MQTT-3.14.4-2]</li>
     */
    @Override
    public void disconnect() {
        //DISCONNECT ???????????????????????????????????????????????????????????????????????????????????????????????????
        write(new MqttDisconnectMessage());
        //??????????????????
        clientConfigure.setAutomaticReconnect(false);
        disconnect = true;
        client.shutdown();
    }

}
