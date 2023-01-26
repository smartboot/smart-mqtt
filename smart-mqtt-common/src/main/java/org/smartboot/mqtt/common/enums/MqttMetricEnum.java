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
    PACKETS_CONNACK_SENT("packets_connack_sent", "发送的 CONNACK 报文数量");

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
