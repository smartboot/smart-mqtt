package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * 服务端存储的消息
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class StoredMessage extends ToString {
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
     * 消息的发送方
     */
    private final String clientID;
    /**
     * 存储的消息偏移量
     */
    private final long offset;

    public StoredMessage(MqttPublishMessage message, String clientId, long offset) {
        this(message.getVariableHeader().getTopicName(), message.getFixedHeader().getQosLevel(), message.getPayload(), message.getFixedHeader().isRetain(), clientId, offset);
    }

    public StoredMessage(String topic, MqttQoS mqttQoS, byte[] payload, boolean retained, String clientID, long offset) {
        this.mqttQoS = mqttQoS;
        this.payload = payload;
        this.topic = topic;
        this.retained = retained;
        this.clientID = clientID;
        this.offset = offset;
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


    public String getClientID() {
        return clientID;
    }


    public long getOffset() {
        return offset;
    }

}
