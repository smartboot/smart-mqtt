/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.util;

/**
 *
 */
public class MqttPropertyConstant {
    private static int bit = 0;
    /**
     * 载荷格式指示
     */
    public static final byte PAYLOAD_FORMAT_INDICATOR = 0x01;
    public static final int PAYLOAD_FORMAT_INDICATOR_BIT = 1 << (bit++);

    /**
     * 消息过期间隔
     */
    public static final byte MESSAGE_EXPIRY_INTERVAL = 0x02;
    public static final int MESSAGE_EXPIRY_INTERVAL_BIT = 1 << (bit++);
    /**
     * 内容类型
     */
    public static final byte CONTENT_TYPE = 0x03;
    public static final int CONTENT_TYPE_BIT = 1 << (bit++);
    /**
     * 响应主题
     */
    public static final byte RESPONSE_TOPIC = 0x08;
    public static final int RESPONSE_TOPIC_BIT = 1 << (bit++);
    /**
     * 对比数据
     */
    public static final byte CORRELATION_DATA = 0x09;
    public static final int CORRELATION_DATA_BIT = 1 << (bit++);
    /**
     * 订阅标识符
     */
    public static final byte SUBSCRIPTION_IDENTIFIER = 0x0B;
    public static final int SUBSCRIPTION_IDENTIFIER_BIT = 1 << (bit++);
    /**
     * 会话过期间隔
     */
    public static final byte SESSION_EXPIRY_INTERVAL = 0x11;
    public static final int SESSION_EXPIRY_INTERVAL_BIT = 1 << (bit++);

    /**
     * 分配客户标识符
     */
    public static final byte ASSIGNED_CLIENT_IDENTIFIER = 0x12;
    public static final int ASSIGNED_CLIENT_IDENTIFIER_BIT = 1 << (bit++);

    /**
     * 服务端保持连接
     */
    public static final byte SERVER_KEEP_ALIVE = 0x13;
    public static final int SERVER_KEEP_ALIVE_BIT = 1 << (bit++);
    /**
     * 认证方法
     */
    public static final byte AUTHENTICATION_METHOD = 0x15;
    public static final int AUTHENTICATION_METHOD_BIT = 1 << (bit++);
    /**
     * 认证数据
     */
    public static final byte AUTHENTICATION_DATA = 0x16;
    public static final int AUTHENTICATION_DATA_BIT = 1 << (bit++);
    //请求问题信息
    public static final byte REQUEST_PROBLEM_INFORMATION = 0x17;
    public static final int REQUEST_PROBLEM_INFORMATION_BIT = 1 << (bit++);

    /**
     * 遗嘱延时间隔
     */
    public static final byte WILL_DELAY_INTERVAL = 0x18;
    public static final int WILL_DELAY_INTERVAL_BIT = 1 << (bit++);

    /**
     * 请求响应信息
     */
    public static final byte REQUEST_RESPONSE_INFORMATION = 0x19;
    public static final int REQUEST_RESPONSE_INFORMATION_BIT = 1 << (bit++);

    /**
     * 响应信息
     */
    public static final byte RESPONSE_INFORMATION = 0x1A;
    public static final int RESPONSE_INFORMATION_BIT = 1 << (bit++);

    /**
     * 服务端参考
     */
    public static final byte SERVER_REFERENCE = 0x1C;
    public static final int SERVER_REFERENCE_BIT = 1 << (bit++);
    /**
     * 原因字符串
     */
    public static final byte REASON_STRING = 0x1F;
    public static final int REASON_STRING_BIT = 1 << (bit++);
    //接收最大值
    public static final byte RECEIVE_MAXIMUM = 0x21;
    public static final int RECEIVE_MAXIMUM_BIT = 1 << (bit++);
    //主题别名最大值
    public static final byte TOPIC_ALIAS_MAXIMUM = 0x22;
    public static final int TOPIC_ALIAS_MAXIMUM_BIT = 1 << (bit++);
    /**
     * 主题别名
     */
    public static final byte TOPIC_ALIAS = 0x23;
    public static final int TOPIC_ALIAS_BIT = 1 << (bit++);

    /**
     * 最大服务质量
     */
    public static final byte MAXIMUM_QOS = 0x24;
    public static final int MAXIMUM_QOS_BIT = 1 << (bit++);
    /**
     * 保留可用
     */
    public static final byte RETAIN_AVAILABLE = 0x25;
    public static final int RETAIN_AVAILABLE_BIT = 1 << (bit++);
    //用户属性
    public static final byte USER_PROPERTY = 0x26;
    public static final int USER_PROPERTY_BIT = 1 << (bit++);
    //最大报文长度
    public static final byte MAXIMUM_PACKET_SIZE = 0x27;
    public static final int MAXIMUM_PACKET_SIZE_BIT = 1 << (bit++);

    /**
     * 通配符订阅可用
     */
    public static final byte WILDCARD_SUBSCRIPTION_AVAILABLE = 0x28;
    public static final int WILDCARD_SUBSCRIPTION_AVAILABLE_BIT = 1 << (bit++);

    /**
     * 订阅标识符可用
     */
    public static final byte SUBSCRIPTION_IDENTIFIER_AVAILABLE = 0x29;
    public static final int SUBSCRIPTION_IDENTIFIER_AVAILABLE_BIT = 1 << (bit++);

    /**
     * 共享订阅可用
     */
    public static final byte SHARED_SUBSCRIPTION_AVAILABLE = 0x2A;
    public static final int SHARED_SUBSCRIPTION_AVAILABLE_BIT = 1 << (bit++);
}