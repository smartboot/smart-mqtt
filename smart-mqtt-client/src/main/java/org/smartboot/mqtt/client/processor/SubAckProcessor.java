package org.smartboot.mqtt.client.processor;

import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;

public class SubAckProcessor implements MqttProcessor<MqttSubAckMessage> {
    @Override
    public void process(MqttClientSession session, MqttSubAckMessage mqttSubAckMessage) {
        MqttFixedHeader mqttFixedHeader = mqttSubAckMessage.getMqttFixedHeader();
    }
}
