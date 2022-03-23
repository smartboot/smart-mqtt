package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttPubCompMessage;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

import static org.smartboot.socket.mqtt.enums.MqttQoS.AT_LEAST_ONCE;

public class PubRelProcessor implements MqttProcessor<MqttPubRelMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubRelMessage mqttPubRelMessage) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, AT_LEAST_ONCE, false, 0);
        MqttPubCompMessage compMessage = new MqttPubCompMessage(fixedHeader, mqttPubRelMessage.getPacketId());
        session.write(compMessage);
    }
}
