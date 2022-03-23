package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttPubRecMessage;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

public class PubRecProcessor implements MqttProcessor<MqttPubRecMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubRecMessage mqttPubrecMessage) {
        MqttPubRelMessage mqttPubRelMessage = new MqttPubRelMessage(mqttPubrecMessage.getPacketId());
        session.write(mqttPubRelMessage);
    }
}
