/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.EventType;
import org.smartboot.mqtt.broker.provider.impl.session.SessionState;
import org.smartboot.mqtt.broker.topic.BrokerTopic;
import org.smartboot.mqtt.broker.topic.TopicSubscriber;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private ConnectProperties properties;

    TimerTask idleConnectTimer;
    /**
     * 最近一次收到客户端消息的时间
     */
    private long latestReceiveMessageTime;

    public MqttSession(BrokerContext mqttContext, AioSession session, MqttWriter mqttWriter) {
        super(mqttContext.getTimer());
        this.mqttContext = mqttContext;
        this.session = session;
        this.mqttWriter = mqttWriter;
        idleConnectTimer = mqttContext.getTimer().schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (!isAuthorized()) {
                    LOGGER.info("长时间未收到客户端：{} 的Connect消息，连接断开！", getClientId());
                    disconnect();
                }
            }
        }, mqttContext.getBrokerConfigure().getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
        mqttContext.getEventBus().publish(EventType.SESSION_CREATE, this);
    }

    public ConnectProperties getProperties() {
        return properties;
    }

    public void setProperties(ConnectProperties properties) {
        this.properties = properties;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    @Override
    public synchronized void write(MqttMessage mqttMessage, boolean autoFlush) {
        mqttContext.getEventBus().publish(EventType.WRITE_MESSAGE, EventObject.newEventObject(this, mqttMessage));
        super.write(mqttMessage, autoFlush);
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
                subscribers.values().forEach(topicSubscriber -> sessionState.getSubscribers().put(topicSubscriber.getTopicFilterToken().getTopicFilter(), topicSubscriber.getMqttQoS()));
                mqttContext.getProviders().getSessionStateProvider().store(clientId, sessionState);
            }
        }

        if (willMessage != null) {
            //非正常中断，推送遗嘱消息
            mqttContext.getMessageBus().publish(this, willMessage);
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
            subscribe0(topicFilter, mqttQoS);
            return mqttQoS;
        } else {
            return MqttQoS.FAILURE;
        }
    }

    private void subscribe0(String topicFilter, MqttQoS mqttQoS) {
        TopicFilterSubscriber subscriber = subscribers.get(topicFilter);
        if (subscriber != null) {
            subscriber.setMqttQoS(mqttQoS);
            subscriber.getTopicSubscribers().values().forEach(sub -> sub.setMqttQoS(mqttQoS));
            return;
        }
        TopicToken topicToken = new TopicToken(topicFilter);
        if (!topicToken.isWildcards()) {
            mqttContext.getOrCreateTopic(topicFilter);
        }
        subscriber = new TopicFilterSubscriber(topicToken, mqttQoS);
        TopicFilterSubscriber preSubscriber = subscribers.put(topicFilter, subscriber);
        ValidateUtils.isTrue(preSubscriber == null, "duplicate topic filter");
        mqttContext.getTopicSubscribeTree().subscribeTopic(this, subscriber);
        mqttContext.getPublishTopicTree().match(topicToken, topic -> subscribeSuccess(mqttQoS, topicToken, topic));
    }

    public void subscribeSuccess(MqttQoS mqttQoS, TopicToken topicToken, BrokerTopic topic) {
        if (!mqttContext.getProviders().getSubscribeProvider().matchTopic(topic, this)) {
            return;
        }
        TopicSubscriber topicSubscriber = topic.getConsumeOffsets().get(this);
        if (topicSubscriber != null) {
            //此前的订阅关系
            TopicToken preToken = topicSubscriber.getTopicFilterToken();
            if (preToken.isWildcards()) {
                if (!topicToken.isWildcards() || topicToken.getTopicFilter().length() > preToken.getTopicFilter().length()) {
                    //解除旧的订阅关系
                    TopicSubscriber preSubscription = subscribers.get(preToken.getTopicFilter()).getTopicSubscribers().remove(topic);
                    preSubscription.setMqttQoS(mqttQoS);
                    preSubscription.setTopicFilterToken(topicToken);
                    //绑定新的订阅关系
                    subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, preSubscription);
                    mqttContext.getEventBus().publish(EventType.SUBSCRIBE_REFRESH_TOPIC, preSubscription);
                }
            }
            return;
        }
        //以当前消息队列的最新点位为起始点位
        TopicSubscriber subscription = new TopicSubscriber(topic, MqttSession.this, mqttQoS, topic.getMessageQueue().getLatestOffset() + 1);
        mqttContext.getEventBus().publish(EventType.SUBSCRIBE_TOPIC, subscription);
        subscription.setTopicFilterToken(topicToken);
        topic.getConsumeOffsets().put(MqttSession.this, subscription);
        subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, subscription);
    }

    public void resubscribe() {
        subscribers.values().stream().filter(subscriber -> subscriber.getTopicFilterToken().isWildcards()).forEach(subscriber -> {
            mqttContext.getPublishTopicTree().match(subscriber.getTopicFilterToken(), topic -> subscribeSuccess(subscriber.getMqttQoS(), subscriber.getTopicFilterToken(), topic));
        });
    }

    public void unsubscribe(String topicFilter) {
        TopicFilterSubscriber filterSubscriber = subscribers.remove(topicFilter);
        if (filterSubscriber == null) {
            return;
        }
        filterSubscriber.getTopicSubscribers().values().forEach(subscriber -> {
            TopicSubscriber removeSubscriber = subscriber.getTopic().getConsumeOffsets().remove(this);
            if (subscriber == removeSubscriber) {
                removeSubscriber.disable();
                mqttContext.getEventBus().publish(EventType.UNSUBSCRIBE_TOPIC, removeSubscriber);
                LOGGER.debug("remove subscriber:{} success!", subscriber.getTopic().getTopic());
            } else {
                LOGGER.error("remove subscriber:{} error!", removeSubscriber);
            }
        });
        mqttContext.getTopicSubscribeTree().unsubscribe(this, filterSubscriber);
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

    public BrokerContext getMqttContext() {
        return mqttContext;
    }

    public Map<String, TopicFilterSubscriber> getSubscribers() {
        return subscribers;
    }

    public long getLatestReceiveMessageTime() {
        return latestReceiveMessageTime;
    }

    public void setLatestReceiveMessageTime(long latestReceiveMessageTime) {
        this.latestReceiveMessageTime = latestReceiveMessageTime;
    }
}
