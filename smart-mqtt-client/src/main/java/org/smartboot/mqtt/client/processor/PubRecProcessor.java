package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;

public class PubRecProcessor implements MqttProcessor<MqttPubRecMessage> {
    @Override
    public void process(MqttClientSession session, MqttPubRecMessage mqttPubrecMessage) {
        MqttPubRelMessage mqttPubRelMessage = new MqttPubRelMessage(mqttPubrecMessage.getPacketId());
        session.write(mqttPubRelMessage);
    }
}
