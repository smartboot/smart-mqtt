package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

public class PubAckProcessor implements MqttProcessor<MqttPubAckMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubAckMessage mqttPubAckMessage) {

    }
}
