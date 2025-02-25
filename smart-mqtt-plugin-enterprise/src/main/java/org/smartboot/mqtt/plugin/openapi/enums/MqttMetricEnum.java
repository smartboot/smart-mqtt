/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.openapi.enums;

/**
 * @author 三刀（zhengjunweimail@163_com）
 * @version V1_0 , 2023/1/26
 */
public enum MqttMetricEnum {
    CLIENT_ONLINE("client_online", "客户端在线数", false),
    CLIENT_CONNECT("client_connect", "客户端连接次数"),
    CLIENT_DISCONNECT("client_disconnected", "客户端断开连接次数"),
    CLIENT_SUBSCRIBE("client_subscribe", "订阅次数"),
    CLIENT_UNSUBSCRIBE("client_unsubscribe", "取消订阅次数"),

    SUBSCRIBE_RELATION("subscribe_relation", "订阅关系数", false),

    BYTES_RECEIVED("bytes_received", "已接收字节数"),
    BYTES_SENT("bytes_sent", "已发送字节数"),

    PACKETS_CONNECT_RECEIVED("packets_connect_received", "接收的 CONNECT 报文数量"),
    PACKETS_CONNACK_SENT("packets_connack_sent", "发送的 CONNACK 报文数量"),

    PACKETS_PUBLISH_RECEIVED("packets_publish_received", "接收的 PUBLISH 报文数量"),
    PACKETS_EXPECT_PUBLISH_SENT("packets_expect_publish_sent", "期望发送的 PUBLISH 报文数量"),
    PACKETS_PUBLISH_SENT("packets_publish_sent", "发送的 PUBLISH 报文数量"),
    PACKETS_PUBLISH_RATE("packets_publish_rate", "消息推送率",false ),

    PACKETS_RECEIVED("packets_received", "接收的报文数量"),
    PACKETS_SENT("packets_sent", "发送的报文数量"),


    TOPIC_COUNT("topic_count", "Topic数量", false),

    MESSAGE_QOS0_RECEIVED("messages_qos0_received", "接收来自客户端的 QoS 0 消息数量"),
    MESSAGE_QOS1_RECEIVED("messages_qos1_received", "接收来自客户端的 QoS 1 消息数量"),
    MESSAGE_QOS2_RECEIVED("messages_qos2_received", "接收来自客户端的 QoS 2 消息数量"),
    MESSAGE_QOS0_SENT("messages_qos0_sent", "发送给客户端的 QoS 0 消息数量"),
    MESSAGE_QOS1_SENT("messages_qos1_sent", "发送给客户端的 QoS 1 消息数量"),
    MESSAGE_QOS2_SENT("messages_qos2_sent", "发送给客户端的 QoS 2 消息数量"),

    PERIOD_MESSAGE_RECEIVED("period_message_received", "周期内接收消息数"),

    PERIOD_MESSAGE_SENT("period_message_sent", "周期内发送消息数");

    private final String code;
    private final String desc;

    private final boolean periodRest;

    MqttMetricEnum(String code, String desc) {
        this(code, desc, true);
    }

    MqttMetricEnum(String code, String desc, boolean periodRest) {
        this.code = code;
        this.desc = desc;
        this.periodRest = periodRest;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isPeriodRest() {
        return periodRest;
    }

    public static MqttMetricEnum getByCode(String code) {
        for (MqttMetricEnum metricEnum : values()) {
            if (metricEnum.code.equals(code)) {
                return metricEnum;
            }
        }
        return null;
    }

}
