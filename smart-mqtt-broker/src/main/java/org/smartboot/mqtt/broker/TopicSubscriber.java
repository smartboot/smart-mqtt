package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class TopicSubscriber {
    private final MqttSession mqttSession;
    /**
     * 定义消息主题
     */
    private final BrokerTopic topic;
    /**
     * 服务端向客户端发送应用消息所允许的最大 QoS 等级
     */
    private final MqttQoS mqttQoS;
    /**
     * 是否可用
     */
    private boolean enable = true;

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
    }

    public BrokerTopic getTopic() {
        return topic;
    }

    public MqttSession getMqttSession() {
        return mqttSession;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }
}
