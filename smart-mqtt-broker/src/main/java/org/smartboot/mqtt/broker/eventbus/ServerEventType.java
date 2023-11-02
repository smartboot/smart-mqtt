/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus;

import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.broker.topic.BrokerTopic;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class ServerEventType<T> extends EventType<T> {

    /**
     * Broker服务启动成功
     */
    public static final ServerEventType<BrokerContext> BROKER_STARTED = new ServerEventType<>("brokerStarted");

    public static final ServerEventType<BrokerConfigure> BROKER_CONFIGURE_LOADED = new ServerEventType<>("brokerConfigureLoaded");

    public static final ServerEventType<BrokerContext> OPEN_API_STARTED = new ServerEventType<>("open_api_started");

    /**
     * 停止Broker服务
     */
    public static final ServerEventType<BrokerContext> BROKER_DESTROY = new ServerEventType<>("brokerDestroy");
    /**
     * 创建MqttSession对象
     */
    public static final ServerEventType<MqttSession> SESSION_CREATE = new ServerEventType<>("sessionCreate");

    /**
     * 创建新Topic
     */
    public static final ServerEventType<BrokerTopic> TOPIC_CREATE = new ServerEventType<>("topicCreate");

    /**
     * Broker接收到客户端发送过来的消息
     */
    public static final ServerEventType<EventObject<MqttPublishMessage>> RECEIVE_PUBLISH_MESSAGE = new ServerEventType<>("receivePublishMessage");

    /**
     * 接受订阅请求
     */
    public static final ServerEventType<EventObject<MqttTopicSubscription>> SUBSCRIBE_ACCEPT = new ServerEventType<>("subscribeAccept");
    /**
     * 接受取消订阅的请求
     */
    public static final ServerEventType<EventObject<MqttUnsubscribeMessage>> UNSUBSCRIBE_ACCEPT = new ServerEventType<>("unsubscribeAccept");

    /**
     * 客户端订阅Topic
     */
    public static final ServerEventType<TopicSubscriber> SUBSCRIBE_TOPIC = new ServerEventType<>("subscribeTopic");

    /**
     * 客户端取消订阅Topic
     */
    public static final ServerEventType<TopicSubscriber> UNSUBSCRIBE_TOPIC = new ServerEventType<>("unsubscribe_topic");

    /**
     * 客户端订阅Topic
     */
    public static final ServerEventType<TopicSubscriber> SUBSCRIBE_REFRESH_TOPIC = new ServerEventType<>("subscribe_refresh_topic");

    /**
     * 消息总线消费完成
     */
    public static final ServerEventType<BrokerTopic> MESSAGE_BUS_CONSUMED = new ServerEventType<>("messageBusProduced");

    /**
     * 客户端连接请求
     */
    public static final ServerEventType<EventObject<MqttConnectMessage>> CONNECT = new ServerEventType<>("connect");

    /**
     * 连接响应
     */
    public static final ServerEventType<EventObject<MqttConnectMessage>> CONNACK = new ServerEventType<>("connect");

    public static final ServerEventType<BrokerTopic> NOTIFY_TOPIC_PUSH = new ServerEventType<>("notify_topic_push");

    protected ServerEventType(String name) {
        super(name);
    }

    /**
     * 这行神奇的代码不要动
     *
     * @return
     */
    public static List<EventType<?>> types() {
        return EventType.types();
    }
}
