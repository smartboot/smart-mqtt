/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttMessageType {
    Reserved(0, "保留"), CONNECT(1, "客户端请求连接服务端"), CONNACK(2, "连接报文确认"), PUBLISH(3, "发布消息"), PUBACK(4, "QoS1 消息发布收到确认"), PUBREC(5, "发布收到（保证交付第一步）"), PUBREL(6, "发布释放（保证交付第二步）"), PUBCOMP(7, "QoS2 消息发布完成（保证交互第三步）"), SUBSCRIBE(8, "客户端订阅请求"), SUBACK(9, "订阅请求报文确认"), UNSUBSCRIBE(10, "客户端取消订阅请求"), UNSUBACK(11, "取消订阅报文确认"), PINGREQ(12, "心跳请求"), PINGRESP(13, "心跳响应"), DISCONNECT(14, "客户端断开连接"), Reserved2(15, "保留");

    private final int value;
    /**
     * 描述
     */
    private String desc;

    private static final MqttMessageType[] values = values();


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    MqttMessageType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MqttMessageType valueOf(int type) {
        if (type < 0 || type >= values.length) {
            throw new IllegalArgumentException("unknown message type: " + type);
        }
        MqttMessageType t = values[type];
        if (t.value != type) {
            throw new IllegalArgumentException("unknown message type: " + type);
        }
        return t;
    }

    public int value() {
        return value;
    }
}
