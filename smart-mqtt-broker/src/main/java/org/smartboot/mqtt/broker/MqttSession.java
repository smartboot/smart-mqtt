package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.plugin.provider.impl.session.SessionState;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.TopicTokenUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话，客户端和服务端之间的状态交互。
 * 一些会话持续时长与网络连接一样，另一些可以在客户端和服务端的多个连续网络连接间扩展。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSession extends AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSession.class);

    /**
     * 当前连接订阅的Topic的消费信息
     */
    private final Map<String, TopicFilterSubscriber> subscribers = new ConcurrentHashMap<>();

    private final BrokerContext mqttContext;
    private final InflightQueue inflightQueue;
    private String username;
    /**
     * 已授权
     */
    private boolean authorized;
    /**
     * 遗嘱消息
     */
    private MqttPublishMessage willMessage;
    private boolean cleanSession;

    public MqttSession(BrokerContext mqttContext, AioSession session, QosPublisher qosPublisher, MqttWriter mqttWriter) {
        super(qosPublisher, mqttContext.getEventBus());
        this.mqttContext = mqttContext;
        this.session = session;
        this.mqttWriter = mqttWriter;
        this.inflightQueue = new InflightQueue(mqttContext.getBrokerConfigure().getMaxInflight());
        mqttContext.getEventBus().publish(ServerEventType.SESSION_CREATE, this);
    }


    public InflightQueue getInflightQueue() {
        return inflightQueue;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public synchronized void disconnect() {
        if (isDisconnect()) {
            return;
        }
        if (isAuthorized()) {
            if (cleanSession) {
                mqttContext.getProviders().getSessionStateProvider().remove(clientId);
            } else {
                //当清理会话标志为 0 的会话连接断开之后，服务端必须将之后的 QoS 1 和 QoS 2 级别的消息保存为会话状态的一部分，
                // 如果这些消息匹配断开连接时客户端的任何订阅
                SessionState sessionState = new SessionState();
                sessionState.getResponseConsumers().putAll(responseConsumers);
                subscribers.values().forEach(topicSubscriber -> sessionState.getSubscribers().put(topicSubscriber.getTopicFilterToken().getTopicFilter(), topicSubscriber.getMqttQoS()));
                mqttContext.getProviders().getSessionStateProvider().store(clientId, sessionState);
            }
        }

        if (willMessage != null) {
            //非正常中断，推送遗嘱消息
            mqttContext.getMessageBus().consume(mqttContext, willMessage);
//            mqttContext.publish( willMessage.getVariableHeader().getTopicName());
        }
        subscribers.keySet().forEach(this::unsubscribe);
        MqttSession removeSession = mqttContext.removeSession(this.getClientId());
        if (removeSession != null && removeSession != this) {
            LOGGER.error("remove old session success:{}", removeSession);
            removeSession.disconnect();
        }
        LOGGER.debug("remove mqttSession success:{}", removeSession);
        disconnect = true;
        try {
            session.close(false);
        } finally {
            mqttContext.getEventBus().publish(EventType.DISCONNECT, this);
        }
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public MqttQoS subscribe(String topicFilter, MqttQoS mqttQoS) {
        if (mqttContext.getProviders().getSubscribeProvider().subscribeTopic(topicFilter, this)) {
            subscribe0(topicFilter, mqttQoS, true);
            return mqttQoS;
        } else {
            return MqttQoS.FAILURE;
        }
    }

    private void subscribe0(String topicFilter, MqttQoS mqttQoS, boolean newSubscribe) {
        TopicToken topicToken = new TopicToken(topicFilter);
        //精准匹配
        if (!topicToken.isWildcards()) {
            BrokerTopic topic = mqttContext.getOrCreateTopic(topicToken.getTopicFilter());//可能会先触发TopicFilterSubscriber.subscribe
            TopicSubscriber subscription = subscribeSuccess(mqttQoS, topicToken, topic);
            if (newSubscribe) {
                mqttContext.getEventBus().publish(ServerEventType.SUBSCRIBE_TOPIC, subscription);
            }
            return;
        }

        //通配符匹配存量Topic
        for (BrokerTopic topic : mqttContext.getTopics()) {
            if (TopicTokenUtil.match(topic.getTopicToken(), topicToken) && mqttContext.getProviders().getSubscribeProvider().subscribeTopic(topic.getTopic(), this)) {
                TopicSubscriber subscription = subscribeSuccess(mqttQoS, topicToken, topic);
                if (newSubscribe) {
                    mqttContext.getEventBus().publish(ServerEventType.SUBSCRIBE_TOPIC, subscription);
                }
            }
        }

        //通配符匹配增量Topic
        if (!subscribers.containsKey(topicFilter)) {
            subscribers.put(topicFilter, new TopicFilterSubscriber(topicToken, mqttQoS));
        }
        if (newSubscribe) {
            mqttContext.getEventBus().subscribe(ServerEventType.TOPIC_CREATE, new EventBusSubscriber<BrokerTopic>() {
                @Override
                public boolean enable() {
                    boolean enable = !disconnect && subscribers.containsKey(topicFilter);
                    if (!enable) {
                        LOGGER.info("current event is disable,quit topic:{} monitor", topicFilter);
                    }
                    return enable;
                }

                @Override
                public void subscribe(EventType<BrokerTopic> eventType, BrokerTopic object) {
                    if (TopicTokenUtil.match(object.getTopicToken(), topicToken) && mqttContext.getProviders().getSubscribeProvider().subscribeTopic(object.getTopic(), MqttSession.this)) {
                        TopicSubscriber subscription = MqttSession.this.subscribeSuccess(mqttQoS, topicToken, object);
                        mqttContext.getEventBus().publish(ServerEventType.SUBSCRIBE_TOPIC, subscription);
                    }
                }
            });
        }
    }

    /**
     * retain消息消费点位记录
     */
    private final Map<BrokerTopic, Long> retainOffsetCache = new HashMap<>();

    private TopicSubscriber subscribeSuccess(MqttQoS mqttQoS, TopicToken topicToken, BrokerTopic topic) {
        long latestOffset = mqttContext.getProviders().getPersistenceProvider().getLatestOffset(topic.getTopic());
        // retain消费点位优先以缓存为准
        Long retainOffset = retainOffsetCache.get(topic);
        long oldestRetainOffset = mqttContext.getProviders().getRetainMessageProvider().getOldestOffset(topic.getTopic());
        if (retainOffset == null || retainOffset < oldestRetainOffset) {
            retainOffset = oldestRetainOffset;
        }
        //以当前消息队列的最新点位为起始点位
        TopicSubscriber subscription = new TopicSubscriber(topic, this, mqttQoS, latestOffset + 1, retainOffset);
        subscription.setTopicFilterToken(topicToken);
        // 如果服务端收到一个 SUBSCRIBE 报文，
        //报文的主题过滤器与一个现存订阅的主题过滤器相同，
        // 那么必须使用新的订阅彻底替换现存的订阅。
        // 新订阅的主题过滤器和之前订阅的相同，但是它的最大 QoS 值可以不同。
        ValidateUtils.isTrue(!disconnect, "session has closed,can not subscribe topic");

        subscribers.values().forEach(topicFilterSubscriber -> {
            TopicSubscriber oldOffset = topicFilterSubscriber.getTopicSubscribers().remove(topic.getTopic());
            if (oldOffset != null) {
                TopicSubscriber consumerOffset = oldOffset.getTopic().getConsumeOffsets().remove(this);
                LOGGER.info("remove topic:{} {},", topic, oldOffset == consumerOffset ? "success" : "fail");
            }
        });

        TopicFilterSubscriber topicFilterSubscriber = subscribers.get(subscription.getTopicFilterToken().getTopicFilter());
        if (topicFilterSubscriber == null) {
            topicFilterSubscriber = new TopicFilterSubscriber(subscription.getTopicFilterToken(), subscription.getMqttQoS(), subscription);
            subscribers.put(subscription.getTopicFilterToken().getTopicFilter(), topicFilterSubscriber);
        } else {
            topicFilterSubscriber.getTopicSubscribers().put(subscription.getTopic().getTopic(), subscription);
        }
        TopicSubscriber preTopicSubscriber = subscription.getTopic().getConsumeOffsets().put(this, subscription);
        if (preTopicSubscriber != null) {
            LOGGER.error("invalid state...");
        } else {
            LOGGER.debug("new subscribe topic:{} success by topicFilter:{}", subscription.getTopic().getTopic(), subscription.getTopicFilterToken().getTopicFilter());
        }

        return subscription;
    }


    public void resubscribe() {
        subscribers.values().forEach(subscriber -> subscribe0(subscriber.getTopicFilterToken().getTopicFilter(), subscriber.getMqttQoS(), false));
    }

    public void unsubscribe(String topicFilter) {
        TopicFilterSubscriber filterSubscriber = subscribers.remove(topicFilter);
        if (filterSubscriber == null) {
            return;
        }
        filterSubscriber.getTopicSubscribers()
                .values().forEach(subscriber -> {
                    TopicSubscriber removeSubscriber = subscriber.getTopic().getConsumeOffsets().remove(this);
                    retainOffsetCache.put(subscriber.getTopic(), subscriber.getRetainConsumerOffset());
                    if (subscriber == removeSubscriber) {
                        LOGGER.debug("remove subscriber:{} success!", subscriber.getTopic().getTopic());
                    } else {
                        LOGGER.error("remove subscriber:{} error!", removeSubscriber);
                    }
                });
    }


    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public void setWillMessage(MqttPublishMessage willMessage) {
        this.willMessage = willMessage;
    }


}
