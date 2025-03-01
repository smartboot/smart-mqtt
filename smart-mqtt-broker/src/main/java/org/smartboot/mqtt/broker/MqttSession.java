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
import org.smartboot.mqtt.broker.eventbus.EventBus;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.EventType;
import org.smartboot.mqtt.broker.provider.impl.session.SessionState;
import org.smartboot.mqtt.broker.topic.AbstractConsumerRecord;
import org.smartboot.mqtt.broker.topic.BrokerTopic;
import org.smartboot.mqtt.broker.topic.SubscriberGroup;
import org.smartboot.mqtt.broker.topic.TopicConsumerRecord;
import org.smartboot.mqtt.broker.topic.TopicQosConsumerRecord;
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
    private final Map<String, TopicSubscriber> subscribers = new ConcurrentHashMap<>();

    private final BrokerContext mqttContext;
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
        }, mqttContext.Options().getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
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
    public void accepted(MqttPublishMessage mqttMessage) {
        mqttContext.getMessageBus().publish(this, mqttMessage);
    }

    @Override
    public void write(MqttMessage mqttMessage, boolean autoFlush) {
        super.write(mqttMessage, autoFlush);
        if (!EventBus.WRITE_MESSAGE_SUBSCRIBER_LIST.isEmpty()) {
            mqttContext.getEventBus().publish(EventType.WRITE_MESSAGE, EventObject.newEventObject(this, mqttMessage), EventBus.WRITE_MESSAGE_SUBSCRIBER_LIST);
        }
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


    public MqttQoS subscribe(String topicFilter, MqttQoS mqttQoS) {
        if (mqttContext.getProviders().getSubscribeProvider().subscribeTopic(topicFilter, this)) {
            subscribe0(topicFilter, mqttQoS);
            return mqttQoS;
        } else {
            return MqttQoS.FAILURE;
        }
    }

    private void subscribe0(String topicFilter, MqttQoS mqttQoS) {
        TopicSubscriber preSubscriber = subscribers.get(topicFilter);
        //订阅topic已存在，可能只是更新了Qos
        if (preSubscriber != null) {
            preSubscriber.setMqttQoS(mqttQoS);
            return;
        }
        TopicToken topicToken = new TopicToken(topicFilter);
        //非通配符匹配需创建BrokerTopic
        if (!topicToken.isWildcards()) {
            if (topicToken.isShared()) {
                String[] array = topicFilter.split("/", 3);
                if (array.length >= 3) {
                    mqttContext.getOrCreateTopic(array[2]);
                }
            } else {
                mqttContext.getOrCreateTopic(topicFilter);
            }
        }
        TopicSubscriber newSubscriber = new TopicSubscriber(topicToken, mqttQoS);
        ValidateUtils.isTrue(subscribers.put(topicFilter, newSubscriber) == null, "duplicate topic filter");
        mqttContext.getTopicSubscribeTree().subscribeTopic(this, newSubscriber);
        mqttContext.getPublishTopicTree().match(topicToken, topic -> subscribeSuccess(newSubscriber, topic));
    }

    public void subscribeSuccess(TopicSubscriber topicSubscriber, BrokerTopic topic) {
        TopicToken topicToken = topicSubscriber.getTopicFilterToken();
        if (!mqttContext.getProviders().getSubscribeProvider().matchTopic(topic, this)) {
            return;
        }
        SubscriberGroup subscriberGroup = topic.getSubscriberGroup(topicSubscriber.getTopicFilterToken());
        AbstractConsumerRecord consumerRecord = subscriberGroup.getSubscriber(this);
        //共享订阅不会为null
        if (consumerRecord == null) {
            TopicConsumerRecord record = newConsumerRecord(topic, topicSubscriber, topic.getMessageQueue().getLatestOffset() + 1);
            mqttContext.getEventBus().publish(EventType.SUBSCRIBE_TOPIC, EventObject.newEventObject(this, record));
            subscriberGroup.addSubscriber(record);
            subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, record);
            return;
        }
        //此前的订阅关系
        TopicToken preToken = consumerRecord.getTopicFilterToken();
        //此前为统配订阅或者未共享订阅，则更新订阅关系
        if (topicToken.isShared()) {
            ValidateUtils.isTrue(preToken.getTopicFilter().equals(topicSubscriber.getTopicFilterToken().getTopicFilter()), "invalid subscriber");
            TopicConsumerRecord record = new TopicConsumerRecord(topic, MqttSession.this, topicSubscriber, topic.getMessageQueue().getLatestOffset() + 1) {
                @Override
                public void pushToClient() {
                    throw new IllegalStateException();
                }
            };
            subscriberGroup.addSubscriber(record);
            subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, record);
        } else if (preToken.isWildcards()) {
            if (!topicToken.isWildcards() || topicToken.getTopicFilter().length() > preToken.getTopicFilter().length()) {
                //解除旧的订阅关系
                TopicConsumerRecord preRecord = subscribers.get(preToken.getTopicFilter()).getTopicSubscribers().remove(topic);
                ValidateUtils.isTrue(preRecord == consumerRecord, "invalid consumerRecord");
                preRecord.disable();

                //绑定新的订阅关系
                TopicConsumerRecord record = newConsumerRecord(topic, topicSubscriber, preRecord.getNextConsumerOffset());
                subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, record);
                mqttContext.getEventBus().publish(EventType.SUBSCRIBE_REFRESH_TOPIC, record);
            }
        }

    }

    private TopicConsumerRecord newConsumerRecord(BrokerTopic topic, TopicSubscriber topicSubscriber, long nextConsumerOffset) {
        if (topicSubscriber.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
            return new TopicConsumerRecord(topic, this, topicSubscriber, nextConsumerOffset);
        } else {
            return new TopicQosConsumerRecord(topic, this, topicSubscriber, nextConsumerOffset);
        }
    }

    public void resubscribe() {
        subscribers.values().stream().filter(subscriber -> subscriber.getTopicFilterToken().isWildcards()).forEach(subscriber -> mqttContext.getPublishTopicTree().match(subscriber.getTopicFilterToken(), topic -> subscribeSuccess(subscriber, topic)));
    }

    public void unsubscribe(String topicFilter) {
        //移除当前Session的映射关系
        TopicSubscriber filterSubscriber = subscribers.remove(topicFilter);
        if (filterSubscriber == null) {
            LOGGER.warn("unsubscribe waring! topic:{} is not exists", topicFilter);
            return;
        }
        //移除关联Broker中的映射关系
        filterSubscriber.getTopicSubscribers().forEach((brokerTopic, subscriber) -> {
            SubscriberGroup subscriberGroup = brokerTopic.getSubscriberGroup(filterSubscriber.getTopicFilterToken());
            AbstractConsumerRecord consumerRecord = subscriberGroup.removeSubscriber(this);
            //移除后，如果BrokerTopic没有订阅者，则清除消息队列
            if (brokerTopic.subscribeCount() == 0) {
                LOGGER.info("clear topic: {} message queue", brokerTopic.getTopic());
                brokerTopic.getMessageQueue().clear();
            }
            if (subscriber == consumerRecord) {
                consumerRecord.disable();
                mqttContext.getEventBus().publish(EventType.UNSUBSCRIBE_TOPIC, consumerRecord);
                LOGGER.debug("remove subscriber:{} success!", brokerTopic.getTopic());
            } else {
                LOGGER.error("remove subscriber:{} error!", subscriberGroup);
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

    public long getLatestReceiveMessageTime() {
        return latestReceiveMessageTime;
    }

    public void setLatestReceiveMessageTime(long latestReceiveMessageTime) {
        this.latestReceiveMessageTime = latestReceiveMessageTime;
    }
}
