package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;

public class PubAckProcessor implements MqttProcessor<MqttPubAckMessage> {
    @Override
    public void process(MqttClientSession session, MqttPubAckMessage mqttPubAckMessage) {

    }
}
