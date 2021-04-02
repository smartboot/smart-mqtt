package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.message.*;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

import static org.smartboot.socket.mqtt.enums.MqttQoS.AT_LEAST_ONCE;
import static org.smartboot.socket.mqtt.enums.MqttQoS.AT_MOST_ONCE;

public class PubRelProcessor implements MqttProcessor<MqttPubRelMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubRelMessage mqttPubRelMessage) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, AT_LEAST_ONCE, false, 0);
        MqttPubCompMessage compMessage = new MqttPubCompMessage(fixedHeader, mqttPubRelMessage.getMqttMessageIdVariableHeader());
        session.write(compMessage);
    }
}
