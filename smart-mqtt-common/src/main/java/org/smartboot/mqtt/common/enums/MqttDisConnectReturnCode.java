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
 *
 */
public enum MqttDisConnectReturnCode {
    /**
     * 正常关闭连接。不发送遗嘱。
     */
    NORMAL_DISCONNECT((byte) 0x00, "正常断开"),
    /**
     * 客户端希望断开但也需要服务端发布它的遗嘱消息。
     */
    DISCONNECT_WITH_WILL_MESSAGE((byte) 0x04, "包含遗嘱消息的断开"),
    /**
     * 连接被关闭，但发送端不愿意透露原因，或者没有其他适用的原因码。
     */
    UNSPECIFIED_ERROR((byte) 0x80, "未指定错误"),
    /**
     * 收到的报文不符合本规范
     */
    MALFORMED_PACKET((byte) 0x81, "无效的报文"),
    /**
     * 收到意外的或无序的报文。
     */
    PROTOCOL_ERROR((byte) 0x82, "协议错误"),
    /**
     * 收到的报文有效，但根据实现无法进行处理。
     */
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 0x83, "实现指定错误"),
    /**
     * 请求没有被授权
     */
    NOT_AUTHORIZED((byte) 0x87, "未授权"),
    SERVER_BUSY((byte) 0x89, "服务端正忙"),
    SERVER_SHUTTING_DOWN((byte) 0x8B, "服务正关闭"),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D, "保持连接超时"),
    SESSION_TAKEN_OVER((byte) 0x8E, "会话被接管"),
    TOPIC_FILTER_INVALID((byte) 0x8F, "主题过滤器无效"),
    TOPIC_NAME_INVALID((byte) 0x90, "主题名无效"),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 0x93, "超出接收最大值"),
    TOPIC_ALIAS_INVALID((byte) 0x94, "主题别名无效"),
    PACKET_TOO_LARGE((byte) 0x95, "报文过大"),
    MESSAGE_RATE_TOO_HIGH((byte) 0x96, "消息速率过高"),
    QUOTA_EXCEEDED((byte) 0x97, "超出配额"),
    ADMINISTRATIVE_ACTION((byte) 0x98, "管理操作"),
    PAYLOAD_FORMAT_INVALID((byte) 0x99, "载荷格式无效"),
    RETAIN_NOT_SUPPORTED((byte) 0x9A, "不支持保留"),
    QOS_NOT_SUPPORTED((byte) 0x9B, "不支持的QoS等级"),
    USE_ANOTHER_SERVER((byte) 0x9C, "（临时）使用其他服务端"),
    SERVER_MOVED((byte) 0x9D, "服务端已（永久）移动"),
    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0x9E, "不支持共享订阅"),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F, "超出连接速率限制"),
    MAXIMUM_CONNECT_TIME((byte) 0xA0, "最大连接时间"),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1, "不支持订阅标识符"),
    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0xA2, "不支持通配符订阅");

    private final byte code;
    private final String desc;

    private static final MqttDisConnectReturnCode[] values = values();

    MqttDisConnectReturnCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MqttDisConnectReturnCode valueOf(byte b) {
        for (MqttDisConnectReturnCode v : values) {
            if (b == v.code) {
                return v;
            }
        }
        throw new IllegalArgumentException("unknown connect return code: " + (b & 0xFF));
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}