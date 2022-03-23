package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttQoS;

public final class MqttTopicSubscription {

    private final String topicFilter;
    private final MqttQoS qualityOfService;
    /**
     * 消费点位
     */
    private int consumerIndex;

    public MqttTopicSubscription(String topicFilter, MqttQoS qualityOfService) {
        this.topicFilter = topicFilter;
        this.qualityOfService = qualityOfService;
    }

    public String topicName() {
        return topicFilter;
    }

    public MqttQoS qualityOfService() {
        return qualityOfService;
    }

    public int getConsumerIndex() {
        return consumerIndex;
    }

    public void setConsumerIndex(int consumerIndex) {
        this.consumerIndex = consumerIndex;
    }
}