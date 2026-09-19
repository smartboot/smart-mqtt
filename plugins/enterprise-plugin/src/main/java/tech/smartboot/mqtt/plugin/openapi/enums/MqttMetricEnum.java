/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.enums;

/**
 * @author 三刀（zhengjunweimail@163_com）
 * @version V1_0 , 2023/1/26
 */
public enum MqttMetricEnum {
    CLIENT_ONLINE("client_online", "客户端在线数", gauge()),

    CLIENT_CONNECT("client_connect", "客户端连接次数", counter()),
    CLIENT_DISCONNECT("client_disconnected", "客户端断开连接次数", counter()),
    CLIENT_SUBSCRIBE("client_subscribe", "订阅次数", counter()),
    CLIENT_UNSUBSCRIBE("client_unsubscribe", "取消订阅次数", counter()),

    SUBSCRIBE_RELATION("subscribe_relation", "订阅关系数", gauge()),

    BYTES_RECEIVED("bytes_received", "已接收字节数", counter()),
    BYTES_SENT("bytes_sent", "已发送字节数", counter()),

    PACKETS_CONNECT_RECEIVED("packets_connect_received", "接收的 CONNECT 报文数量", counter()),
    PACKETS_CONNACK_SENT("packets_connack_sent", "发送的 CONNACK 报文数量", counter()),

    PACKETS_PUBLISH_RECEIVED("packets_publish_received", "接收的 PUBLISH 报文数量", counter()),
    PACKETS_EXPECT_PUBLISH_SENT("packets_expect_publish_sent", "期望发送的 PUBLISH 报文数量", counter()),
    PACKETS_PUBLISH_SENT("packets_publish_sent", "发送的 PUBLISH 报文数量", counter()),

    PACKETS_RECEIVED("packets_received", "接收的报文数量", counter()),
    PACKETS_SENT("packets_sent", "发送的报文数量", counter()),


    TOPIC_COUNT("topic_count", "Topic数量", gauge()),

    MESSAGE_QOS0_RECEIVED("messages_qos0_received", "接收来自客户端的 QoS 0 消息数量", counter()),
    MESSAGE_QOS1_RECEIVED("messages_qos1_received", "接收来自客户端的 QoS 1 消息数量", counter()),
    MESSAGE_QOS2_RECEIVED("messages_qos2_received", "接收来自客户端的 QoS 2 消息数量", counter()),
    MESSAGE_QOS0_SENT("messages_qos0_sent", "发送给客户端的 QoS 0 消息数量", counter()),
    MESSAGE_QOS1_SENT("messages_qos1_sent", "发送给客户端的 QoS 1 消息数量", counter()),
    MESSAGE_QOS2_SENT("messages_qos2_sent", "发送给客户端的 QoS 2 消息数量", counter()),
    ;

    private final String code;
    private final String desc;

    private static final int FLAG_PROMETHEUS_METRIC_TYPE_COUNTER = 1 << 1;
    private static final int FLAG_PROMETHEUS_METRIC_TYPE_GAUGE = 1 << 2;
    private final int flag;

    /**
     * Prometheus counter 类型：单调递增的累计值
     */
    static int counter() {
        return FLAG_PROMETHEUS_METRIC_TYPE_COUNTER;
    }

    /**
     * Prometheus gauge 类型：可增可减的瞬时值
     */
    static int gauge() {
        return FLAG_PROMETHEUS_METRIC_TYPE_GAUGE;
    }

    MqttMetricEnum(String code, String desc) {
        this(code, desc, counter());
    }

    MqttMetricEnum(String code, String desc, int flag) {
        this.code = code;
        this.desc = desc;
        this.flag = flag;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isPrometheusSupport() {
        return isPrometheusMetricTypeCounter() || isPrometheusMetricTypeGauge();
    }

    public boolean isPrometheusMetricTypeCounter() {
        return (flag & FLAG_PROMETHEUS_METRIC_TYPE_COUNTER) > 0;
    }

    public boolean isPrometheusMetricTypeGauge() {
        return (flag & FLAG_PROMETHEUS_METRIC_TYPE_GAUGE) > 0;
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
