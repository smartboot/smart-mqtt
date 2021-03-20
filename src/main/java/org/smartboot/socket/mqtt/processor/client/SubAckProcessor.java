package org.smartboot.socket.mqtt.processor.client;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttPubRecMessage;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.message.MqttSubAckMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

public class SubAckProcessor implements MqttProcessor<MqttSubAckMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttSubAckMessage mqttSubAckMessage) {
        MqttFixedHeader mqttFixedHeader = mqttSubAckMessage.getMqttFixedHeader();
    }
}
