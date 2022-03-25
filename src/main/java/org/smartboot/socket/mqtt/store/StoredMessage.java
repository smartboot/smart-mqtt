package org.smartboot.socket.mqtt.store;

import org.smartboot.socket.mqtt.ToString;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class StoredMessage extends ToString {
    /**
     * 消息等级
     */
    final MqttQoS mqttQoS;
    /**
     * 负载数据
     */
    final byte[] payload;
    /**
     * 主题
     */
    final String topic;

    private boolean retained;
    private String clientID;
    /**
     * 存储的消息偏移量
     */
    private long offset;

    public StoredMessage(byte[] payload, MqttQoS mqttQoS, String topic) {
        this.mqttQoS = mqttQoS;
        this.payload = payload;
        this.topic = topic;
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

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
