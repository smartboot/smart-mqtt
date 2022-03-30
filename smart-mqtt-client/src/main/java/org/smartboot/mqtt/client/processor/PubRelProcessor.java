package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;

import static org.smartboot.mqtt.common.enums.MqttQoS.AT_LEAST_ONCE;

public class PubRelProcessor implements MqttProcessor<MqttPubRelMessage> {
    @Override
    public void process(MqttClientSession session, MqttPubRelMessage mqttPubRelMessage) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, AT_LEAST_ONCE, false, 0);
        MqttPubCompMessage compMessage = new MqttPubCompMessage(fixedHeader, mqttPubRelMessage.getPacketId());
        session.write(compMessage);
    }
}
