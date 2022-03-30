package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;

public class PubCompProcessor implements MqttProcessor<MqttPubCompMessage> {
    @Override
    public void process(MqttClientSession session, MqttPubCompMessage mqttPubCompMessage) {
    }
}
