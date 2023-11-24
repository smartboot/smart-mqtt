/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线中支持的事件类型
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventType<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventType.class);
    private final List<EventBusSubscriber<T>> subscribers = new CopyOnWriteArrayList<>();
    //客户端连接Broker
    public static final EventType<AbstractSession> CONNECT = new EventType<>("connect");
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
    private final String name;

    protected EventType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "EventType{" +
                "name='" + name + '\'' +
                '}';
    }

    void subscribe(EventBusSubscriber<T> subscriber) {
        subscribers.add(subscriber);
    }


    void publish(T object) {
        boolean remove = false;
        for (EventBusSubscriber<T> subscriber : subscribers) {
            try {
                if (subscriber.enable()) {
                    subscriber.subscribe(this, object);
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                LOGGER.error("publish event error", throwable);
            }
        }
        if (remove) {
            subscribers.removeIf(eventBusSubscriber -> !eventBusSubscriber.enable());
        }
    }
}
