package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public class Message extends ToString {
    /**
     * 消息等级
     */
    private final MqttQoS mqttQoS;
    /**
     * 负载数据
     */
    private final byte[] payload;
    /**
     * 主题
     */
    private final String topic;

    private final boolean retained;

    public Message(Message message) {
        this(message.topic, message.mqttQoS, message.payload, message.retained);
    }

    public Message(MqttPublishMessage message) {
        this(message.getVariableHeader().getTopicName(), message.getFixedHeader().getQosLevel(), message.getPayload(), message.getFixedHeader().isRetain());
    }

    private Message(String topic, MqttQoS mqttQoS, byte[] payload, boolean retained) {
        this.mqttQoS = mqttQoS;
        this.payload = payload;
        this.topic = topic;
        this.retained = retained;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public boolean isRetained() {
        return retained;
    }


}
