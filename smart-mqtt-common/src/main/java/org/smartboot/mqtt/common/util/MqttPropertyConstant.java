package org.smartboot.mqtt.common.util;

/**
 *
 */
public class MqttPropertyConstant {
    /**
     * 载荷格式指示
     */
    public static final byte PAYLOAD_FORMAT_INDICATOR = 0x01;

    /**
     * 消息过期间隔
     */
    public static final byte MESSAGE_EXPIRY_INTERVAL = 0x02;
    /**
     * 内容类型
     */
    public static final byte CONTENT_TYPE = 0x03;
    /**
     * 响应主题
     */
    public static final byte RESPONSE_TOPIC = 0x08;
    /**
     * 对比数据
     */
    public static final byte CORRELATION_DATA = 0x09;
    /**
     * 订阅标识符
     */
    public static final byte SUBSCRIPTION_IDENTIFIER = 0x0B;
    //会话过期间隔
    public static final byte SESSION_EXPIRY_INTERVAL = 0x11;

    /**
     * 分配客户标识符
     */
    public static final byte ASSIGNED_CLIENT_IDENTIFIER = 0x12;

    /**
     * 服务端保持连接
     */
    public static final byte SERVER_KEEP_ALIVE = 0x13;
    /**
     * 认证方法
     */
    public static final byte AUTHENTICATION_METHOD = 0x15;
    /**
     * 认证数据
     */
    public static final byte AUTHENTICATION_DATA = 0x16;
    //请求问题信息
    public static final byte REQUEST_PROBLEM_INFORMATION = 0x17;

    /**
     * 遗嘱延时间隔
     */
    public static final byte WILL_DELAY_INTERVAL = 0x18;

    /**
     * 响应信息
     */
    public static final byte RESPONSE_INFORMATION = 0x1A;

    /**
     * 服务端参考
     */
    public static final byte SERVER_REFERENCE = 0x1C;
    /**
     * 原因字符串
     */
    public static final byte REASON_STRING = 0x1F;
    //请求响应信息
    public static final byte REQUEST_RESPONSE_INFORMATION = 0x19;
    //接收最大值
    public static final byte RECEIVE_MAXIMUM = 0x21;
    //主题别名最大值
    public static final byte TOPIC_ALIAS_MAXIMUM = 0x22;
    /**
     * 主题别名
     */
    public static final byte TOPIC_ALIAS = 0x23;

    /**
     * 最大服务质量
     */
    public static final byte MAXIMUM_QOS = 0x24;
    /**
     * 保留可用
     */
    public static final byte RETAIN_AVAILABLE = 0x25;
    //用户属性
    public static final byte USER_PROPERTY = 0x26;
    //最大报文长度
    public static final byte MAXIMUM_PACKET_SIZE = 0x27;

    /**
     * 通配符订阅可用
     */
    public static final byte WILDCARD_SUBSCRIPTION_AVAILABLE = 0x28;

    /**
     * 订阅标识符可用
     */
    public static final byte SUBSCRIPTION_IDENTIFIER_AVAILABLE = 0x29;

    /**
     * 共享订阅可用
     */
    public static final byte SHARED_SUBSCRIPTION_AVAILABLE = 0x2A;

}