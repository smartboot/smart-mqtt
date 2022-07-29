package org.smartboot.mqtt.broker.messagebus;

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
    /**
     * 消息总线中存储的消息偏移量
     */
    private final long busOffset;

    public Message(Message message, long busOffset) {
        this(message.topic, message.mqttQoS, message.payload, message.retained, busOffset);
    }

    public Message(MqttPublishMessage message, long busOffset) {
        this(message.getVariableHeader().getTopicName(), message.getFixedHeader().getQosLevel(), message.getPayload(), message.getFixedHeader().isRetain(), busOffset);
    }

    private Message(String topic, MqttQoS mqttQoS, byte[] payload, boolean retained, long busOffset) {
        this.mqttQoS = mqttQoS;
        this.payload = payload;
        this.topic = topic;
        this.retained = retained;
        this.busOffset = busOffset;
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


    public long getBusOffset() {
        return busOffset;
    }
}
