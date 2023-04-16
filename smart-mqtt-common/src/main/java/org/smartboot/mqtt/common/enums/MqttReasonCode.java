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
public enum MqttReasonCode {
    //MQTT5
    SUCCESS((byte) 0x00, "成功"),
    NORMAL_DISCONNECTION((byte) 0x00, "断开连接"),
    GRANTED_QOS0((byte) 0x00, "最大允许qos为0"),
    GRANTED_QOS1((byte) 0x01, "最大允许qos为1"),
    GRANTED_QOS2((byte) 0x02, "最大允许qos为2"),
    DISCONNECT_WITH_WILL_MESSAGE((byte) 0x04, "客户端需要断开连接后发送遗嘱消息"),
    NO_MATCHING_SUBSCRIBERS((byte) 0X10, "无匹配的订阅者"),
    N0_SUBSCRIPTION_EXISTED((byte) 0X11, ""),
    CONTINUE_AUTHENTICATION((byte) 0X18, ""),
    RE_AUTHENTICATE((byte) 0X19, ""),
    UNSPECIFIED_ERROR((byte) 0x80, "未指明的错误"),
    MALFORMED_PACKET((byte) 0x81, "数据未正确解析"),
    PROTOCOL_ERROR((byte) 0x82, "协议版本错误"),
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 0x83, "接收者不接受"),
    UNSUPPORTED_PROTOCOL_VERSION((byte) 0x84, "服务端不支持此版本协议"),
    CLIENT_IDENTIFIER_NOT_VALID((byte) 0x85, "不允许的客户端id"),
    BAD_USERNAME_OR_PASSWORD((byte) 0x86, "不接受的用户名或密码"),
    NOT_AUTHORIZED((byte) 0x87, "未授权"),
    SERVER_UNAVAILABLE_5((byte) 0x88, "服务端不可用"),
    SERVER_BUSY((byte) 0x89, "服务端繁忙中"),
    BANNED((byte) 0x8A, "客户端被禁用"),
    SERVER_SHUTTING_DOWN((byte) 0x8B, ""),
    BAD_AUTHENTICATION_METHOD((byte) 0x8C, "错误的认证方法"),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D, ""),
    SESSION_TAKEN_OVER((byte) 0x8E, "相同客户端id上线导致被踢出下线"),
    TOPIC_FILTER_INVALID((byte) 0x8F, "消息过滤非法"),
    TOPIC_NAME_INVALID((byte) 0x90, "topic名非法"),
    PACKET_IDENTIFIER_IN_USE((byte) 0x91, "packetId已被使用"),
    PACKET_IDENTIFIER_NOT_FOUND((byte) 0x92, ""),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 0x93, ""),
    TOPIC_ALIAS_INVALID((byte) 0x94, ""),
    PACKET_TOO_LARGE((byte) 0x95, "包大小超限"),
    MESSAGE_RATE_TOO_HIGH((byte) 0x96, ""),
    QUOTA_EXCEEDED((byte) 0x97, "已超限"),
    ADMINISTRATIVE_ACTION((byte) 0x98, ""),
    PAYLOAD_FORMAT_INVALID((byte) 0x99, "数据格式非法"),
    RETAIN_NOT_SUPPORTED((byte) 0x9A, "不支持保留消息"),
    QOS_NOT_SUPPORTED((byte) 0x9B, "不支持此qos"),
    USE_ANOTHER_SERVER((byte) 0x9C, "客户端需要暂时使用另一节点"),
    SERVER_MOVED((byte) 0x9D, "客户端需要永久使用另一节点"),
    SHARED_SUBSCRIPTION_NOT_SUPPORTED((byte) 0x9E, ""),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F, "连接速率超限"),
    MAXIMUM_CONNECT_TIME((byte) 0xA0, ""),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1, ""),
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED((byte) 0xA2, "");

    private final byte code;
    private final String desc;

    MqttReasonCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MqttReasonCode valueOf(byte b) {
        for (MqttReasonCode v : values()) {
            if (b == v.code) {
                return v;
            }
        }
        throw new IllegalArgumentException("unknown reason code: " + (b & 0xFF));
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}