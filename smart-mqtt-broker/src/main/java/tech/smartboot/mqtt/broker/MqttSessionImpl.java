/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.broker.topic.BaseMessageDeliver;
import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.broker.topic.DeliverGroup;
import tech.smartboot.mqtt.common.AbstractSession;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.MqttWriter;
import tech.smartboot.mqtt.common.TopicToken;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import tech.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MessageDeliver;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.PublishBuilder;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.provider.SessionState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 会话，客户端和服务端之间的状态交互。
 * 一些会话持续时长与网络连接一样，另一些可以在客户端和服务端的多个连续网络连接间扩展。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSessionImpl extends AbstractSession implements MqttSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSessionImpl.class);

    /**
     * 当前连接订阅的Topic的消费信息
     */
    private final Map<String, SessionSubscribeRelation> subscribers = new ConcurrentHashMap<>();

    private final BrokerContextImpl mqttContext;
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


    public MqttSessionImpl(BrokerContextImpl mqttContext, AioSession session, MqttWriter mqttWriter) {
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
        mqttContext.getOriginalMessageBus().publish(this, mqttMessage);
    }

    @Override
    public void write(MqttMessage mqttMessage, boolean autoFlush) {
        super.write(mqttMessage, autoFlush);
        if (!EventBusImpl.WRITE_MESSAGE_SUBSCRIBER_LIST.isEmpty()) {
            mqttContext.getEventBus().publish(EventType.WRITE_MESSAGE, EventObject.newEventObject(this, mqttMessage), EventBusImpl.WRITE_MESSAGE_SUBSCRIBER_LIST);
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
                subscribers.values().forEach(topicSubscription -> sessionState.getSubscribers().put(topicSubscription.getTopicFilterToken().getTopicFilter(), topicSubscription.getMqttQoS()));
                mqttContext.getProviders().getSessionStateProvider().store(clientId, sessionState);
            }
        }

        if (willMessage != null) {
            //非正常中断，推送遗嘱消息
            mqttContext.getOriginalMessageBus().publish(this, willMessage);
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


    public void subscribe(String topicFilter, MqttQoS mqttQoS) {
        SessionSubscribeRelation preSubscriber = subscribers.get(topicFilter);
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
        SessionSubscribeRelation relation = new SessionSubscribeRelation(this, topicToken, mqttQoS);
        ValidateUtils.isTrue(subscribers.put(topicFilter, relation) == null, "duplicate topic filter");
        mqttContext.getRelationMatcher().add(relation);
        mqttContext.getTopicMatcher().match(relation, topic -> subscribeSuccess(relation, topic));
    }

    public void subscribeSuccess(SessionSubscribeRelation sessionSubscribeRelation, BrokerTopicImpl topic) {
        TopicToken topicToken = sessionSubscribeRelation.getTopicFilterToken();
        if (!mqttContext.getProviders().getSubscribeProvider().matchTopic(topic, this)) {
            return;
        }
        DeliverGroup group = topic.getSubscriberGroup(topicToken);
        if (group.isShared()) {
//            MessageDeliver messageDeliver = group.getSubscriber(this);
//            TopicToken preToken = messageDeliver.getTopicFilterToken();
//            ValidateUtils.isTrue(preToken.getTopicFilter().equals(topicSubscription.getTopicFilterToken().getTopicFilter()), "invalid subscriber");
            BaseMessageDeliver record = new BaseMessageDeliver(topic, sessionSubscribeRelation, topic.getMessageQueue().getLatestOffset() + 1);
            group.addMessageDeliver(record);
            subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, record);
            return;
        }
        //从 BrokerTopic 中获取当前连接的MessageDeliver
        MessageDeliver messageDeliver = group.getMessageDeliver(this);
        if (messageDeliver == null) {
            BaseMessageDeliver deliver = BaseMessageDeliver.newMessageDeliver(topic, sessionSubscribeRelation, topic.getMessageQueue().getLatestOffset() + 1);
            //加入推送队列
            addSubscriber(topic, deliver);
            //将 deliver 添加到 topic 的订阅组
            group.addMessageDeliver(deliver);
            //更新当前连接的订阅关系
            subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, deliver);
            mqttContext.getEventBus().publish(EventType.SUBSCRIBE_TOPIC, EventObject.newEventObject(this, deliver));
            return;
        }
        TopicToken preToken = messageDeliver.getSubscribeRelation();
        //此前为统配订阅，则更新订阅关系
        if (preToken.isWildcards() && (!topicToken.isWildcards() || topicToken.getTopicFilter().length() > preToken.getTopicFilter().length())) {
            //解除旧的订阅关系
            BaseMessageDeliver preRecord = subscribers.get(preToken.getTopicFilter()).getTopicSubscribers().remove(topic);
            ValidateUtils.isTrue(preRecord == messageDeliver, "invalid messageDeliver");
            group.removeMessageDeliver(this);

            //绑定新的订阅关系
            BaseMessageDeliver record = BaseMessageDeliver.newMessageDeliver(topic, sessionSubscribeRelation, preRecord.getNextConsumerOffset());
            topic.registerMessageDeliver(record);
            group.addMessageDeliver(record);
            //更新订阅关系
            subscribers.get(topicToken.getTopicFilter()).getTopicSubscribers().put(topic, record);
            mqttContext.getEventBus().publish(EventType.SUBSCRIBE_REFRESH_TOPIC, record);
        }
    }

    //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
    private void addSubscriber(BrokerTopicImpl topic, BaseMessageDeliver deliver) {
        Message retainMessage = topic.getRetainMessage();
        if (retainMessage == null || retainMessage.getCreateTime() > deliver.getLatestSubscribeTime()) {
            topic.registerMessageDeliver(deliver);
            return;
        }

        PublishBuilder publishBuilder = PublishBuilder.builder().payload(retainMessage.getPayload()).qos(deliver.getMqttQoS()).topic(retainMessage.getTopic()).retained(true);
        if (getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }
        // Qos0不走飞行窗口
        if (deliver.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
            write(publishBuilder.build());
            topic.registerMessageDeliver(deliver);
            return;
        }
        // retain消息逐个推送
        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = inflightQueue.offer(publishBuilder);
        future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
            LOGGER.info("publish retain to client:{} success  ", getClientId());
            topic.registerMessageDeliver(deliver);
        });
        flush();
    }

    public void resubscribe() {
        subscribers.values().stream().filter(subscriber -> subscriber.getTopicFilterToken().isWildcards()).forEach(subscriber -> mqttContext.getTopicMatcher().match(subscriber, topic -> subscribeSuccess(subscriber, topic)));
    }

    public void unsubscribe(String topicFilter) {
        //移除当前Session的映射关系
        SessionSubscribeRelation filterSubscriber = subscribers.remove(topicFilter);
        if (filterSubscriber == null) {
            LOGGER.warn("unsubscribe waring! topic:{} is not exists", topicFilter);
            return;
        }
        //移除关联Broker中的映射关系
        filterSubscriber.getTopicSubscribers().forEach((brokerTopic, subscriber) -> {
            DeliverGroup subscriberGroup = brokerTopic.getSubscriberGroup(filterSubscriber.getTopicFilterToken());
            BaseMessageDeliver consumerRecord = subscriberGroup.removeMessageDeliver(this);
            //移除后，如果BrokerTopic没有订阅者，则清除消息队列
            if (brokerTopic.subscribeCount() == 0) {
                LOGGER.info("clear topic: {} message queue", brokerTopic.getTopicFilter());
                brokerTopic.getMessageQueue().clear();
            }
            //正常情况下，当前session中维护的订阅关系与BrokerTopic中的订阅关系是一致的
            //如果不一致，说明可能存在bug
            if (subscriber == consumerRecord) {
                mqttContext.getEventBus().publish(EventType.UNSUBSCRIBE_TOPIC, consumerRecord);
                LOGGER.debug("remove subscriber:{} success!", brokerTopic.getTopicFilter());
            } else {
                LOGGER.error("remove subscriber:{} error!", subscriberGroup);
            }
        });
        mqttContext.getRelationMatcher().remove(filterSubscriber);
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
