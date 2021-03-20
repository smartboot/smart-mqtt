package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttPubCompMessage;
import org.smartboot.socket.mqtt.message.MqttPubRecMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

public class PubCompProcessor implements MqttProcessor<MqttPubCompMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubCompMessage mqttPubCompMessage) {
    }
}
