package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀（zhengjunweimail@163_com）
 * @version V1_0 , 2023/1/26
 */
public enum MqttMetricEnum {
    CLIENT_CONNECT("client_connect", "客户端连接次数"),
    CLIENT_DISCONNECT("client_disconnected", "客户端断开连接次数"),
    CLIENT_SUBSCRIBE("client_subscribe", "订阅次数"),
    CLIENT_UNSUBSCRIBE("client_unsubscribe", "取消订阅次数"),
    BYTES_RECEIVED("bytes_received", "已接收字节数"),
    BYTES_SENT("bytes_sent", "已发送字节数"),

    PACKETS_CONNECT_RECEIVED("packets_connect_received", "接收的 CONNECT 报文数量"),
    PACKETS_CONNACK_SENT("packets_connack_sent", "发送的 CONNACK 报文数量"),
    PACKETS_RECEIVED("packets_connect_received", "接收的报文数量"),
    PACKETS_SENT("packets_connack_sent", "发送的报文数量"),


    MESSAGE_QOS0_RECEIVED("messages_qos0_received", "接收来自客户端的 QoS 0 消息数量"),
    MESSAGE_QOS1_RECEIVED("messages_qos1_received", "接收来自客户端的 QoS 1 消息数量"),
    MESSAGE_QOS2_RECEIVED("messages_qos2_received", "接收来自客户端的 QoS 2 消息数量"),
    MESSAGE_QOS0_SENT("messages_qos0_sent", "发送给客户端的 QoS 0 消息数量"),
    MESSAGE_QOS1_SENT("messages_qos1_sent", "发送给客户端的 QoS 1 消息数量"),
    MESSAGE_QOS2_SENT("messages_qos2_sent", "发送给客户端的 QoS 2 消息数量");

    private final String code;
    private final String desc;

    MqttMetricEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
