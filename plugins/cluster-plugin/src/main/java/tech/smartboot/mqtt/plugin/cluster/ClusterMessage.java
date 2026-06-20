package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.mqtt.plugin.spec.BrokerTopic;
import tech.smartboot.mqtt.plugin.spec.Message;

public class ClusterMessage {
    private final BrokerTopic topic;
    private final Message message;

    public ClusterMessage(BrokerTopic topic, Message message) {
        this.topic = topic;
        this.message = message;
    }

    public BrokerTopic getTopic() {
        return topic;
    }

    public Message getMessage() {
        return message;
    }
}
