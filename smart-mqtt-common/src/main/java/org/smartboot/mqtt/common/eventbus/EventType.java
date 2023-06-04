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

import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 事件总线中支持的事件类型
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventType<T> {
    private static int count = 0;
    private static final List<EventType<?>> list = new ArrayList<>();
    private final int index;
    //客户端连接Broker
    public static final EventType<AbstractSession> CONNECT = new EventType<>("connect");
    //连接断开
    public static final EventType<AbstractSession> DISCONNECT = new EventType<>("disconnect");

    /**
     * 接收到客户端发送的任何消息
     */
    public static final EventType<EventObject<MqttMessage>> RECEIVE_MESSAGE = new EventType<>("receiveMessage");

    /**
     * 往客户端发送的任何消息
     */
    public static final EventType<EventObject<MqttMessage>> WRITE_MESSAGE = new EventType<>("writeMessage");
    private final String name;

    protected EventType(String name) {
        this.name = name;
        index = count++;
        list.add(index, this);
    }

    public static int count() {
        return count;
    }

    public static List<EventType<?>> types() {
        return list;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "EventType{" +
                "name='" + name + '\'' +
                '}';
    }
}
