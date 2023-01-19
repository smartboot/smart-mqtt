package org.smartboot.mqtt.common.enums;

/**
 *
 */
public enum MqttConnectReasonCode {
    //MQTT5
    SUCCESS((byte) 0x00, "成功"),
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
    BAD_AUTHENTICATION_METHOD((byte) 0x8C, "错误的认证方法"),
    TOPIC_NAME_INVALID((byte) 0x90, "topic名非法"),
    PACKET_TOO_LARGE((byte) 0x95, "包大小超限"),
    QUOTA_EXCEEDED((byte) 0x97, "已超限"),
    PAYLOAD_FORMAT_INVALID((byte) 0x99, "数据格式非法"),
    RETAIN_NOT_SUPPORTED((byte) 0x9A, "不支持保留消息"),
    QOS_NOT_SUPPORTED((byte) 0x9B, "不支持此qos"),
    USE_ANOTHER_SERVER((byte) 0x9C, "客户端需要暂时使用另一节点"),
    SERVER_MOVED((byte) 0x9D, "客户端需要永久使用另一节点"),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F, "连接速率超限");

    private final byte code;
    private final String desc;

    MqttConnectReasonCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MqttConnectReasonCode valueOf(byte b) {
        for (MqttConnectReasonCode v : values()) {
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