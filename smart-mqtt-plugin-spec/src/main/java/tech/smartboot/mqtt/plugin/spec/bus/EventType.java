/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.bus;

import tech.smartboot.mqtt.common.AbstractSession;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttTopicSubscription;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MessageDeliver;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;

/**
 * 事件总线中支持的事件类型
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventType<T> {
    //连接断开
    public static final EventType<AbstractSession> DISCONNECT = new EventType<>("disconnect");

    /**
     * 接收到客户端发送的任何消息
     */
    public static final EventType<EventObject<MqttMessage>> RECEIVE_MESSAGE = new EventType<>("receiveMessage");

    /**
     * 接收到客户端发送的任何消息
     */
    public static final EventType<MqttConnAckMessage> RECEIVE_CONN_ACK_MESSAGE = new EventType<>("connAckMessage");

    /**
     * 往客户端发送的任何消息
     */
    public static final EventType<EventObject<MqttMessage>> WRITE_MESSAGE = new EventType<>("writeMessage");
    /**
     * Broker服务启动成功
     */
    public static final EventType<BrokerContext> BROKER_STARTED = new EventType<>("brokerStarted", true);

    public static final EventType<Options> BROKER_CONFIGURE_LOADED = new EventType<>("brokerConfigureLoaded", true);

    /**
     * 停止Broker服务
     */
    public static final EventType<BrokerContext> BROKER_DESTROY = new EventType<>("brokerDestroy", true);
    /**
     * 创建MqttSession对象
     */
    public static final EventType<MqttSession> SESSION_CREATE = new EventType<>("sessionCreate");

    /**
     * 创建新Topic
     */
    public static final EventType<String> TOPIC_CREATE = new EventType<>("topicCreate");

    /**
     * 接受订阅请求
     */
    public static final EventType<EventObject<MqttTopicSubscription>> SUBSCRIBE_ACCEPT = new EventType<>("subscribeAccept");
    /**
     * 接受取消订阅的请求
     */
    public static final EventType<EventObject<MqttUnsubscribeMessage>> UNSUBSCRIBE_ACCEPT = new EventType<>("unsubscribeAccept");

    /**
     * 客户端订阅Topic
     */
    public static final EventType<EventObject<MessageDeliver>> SUBSCRIBE_TOPIC = new EventType<>("subscribeTopic");

    /**
     * 客户端取消订阅Topic
     */
    public static final EventType<MessageDeliver> UNSUBSCRIBE_TOPIC = new EventType<>("unsubscribe_topic");

    /**
     * 客户端订阅Topic
     */
    public static final EventType<MessageDeliver> SUBSCRIBE_REFRESH_TOPIC = new EventType<>("subscribe_refresh_topic");

    /**
     * 客户端连接请求
     */
    public static final EventType<EventObject<MqttConnectMessage>> CONNECT = new EventType<>("connect");
    private final String name;

    /**
     *一次性事件
     */
    private final boolean once;

    EventType(String name, boolean once) {
        this.name = name;
        this.once = once;
    }

    EventType(String name) {
        this(name, false);
    }

    public boolean isOnce() {
        return once;
    }

    @Override
    public String toString() {
        return "EventType{" + "name='" + name + '\'' + '}';
    }
}
