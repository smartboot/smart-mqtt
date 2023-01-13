package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MqttSubscribePayload extends MqttPayload {

    private List<MqttTopicSubscription> topicSubscriptions;
    private List<byte[]> topics;

    public void setTopicSubscriptions(List<MqttTopicSubscription> topicSubscriptions) {
        this.topicSubscriptions = topicSubscriptions;
    }

    public List<MqttTopicSubscription> getTopicSubscriptions() {
        return topicSubscriptions;
    }

    @Override
    protected int preEncode() {
        int length = topicSubscriptions.size();
        topics = new ArrayList<>(topicSubscriptions.size());
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            byte[] bytes = MqttCodecUtil.encodeUTF8(topicSubscription.getTopicFilter());
            topics.add(bytes);
            length += bytes.length;
        }
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        int i = 0;
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            mqttWriter.write(topics.get(i++));
            mqttWriter.writeByte((byte) topicSubscription.getQualityOfService().value());
        }
    }
}