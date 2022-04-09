package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;

/**
 * 连接处理器
 *
 * @author stw
 * @version V1.0 , 2018/4/25
 */
public class ConnAckProcessor implements MqttProcessor<MqttConnAckMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnAckProcessor.class);

    @Override
    public void process(MqttClient client, MqttConnAckMessage mqttConnAckMessage) {
        LOGGER.info("receive connectAck message:{}", mqttConnAckMessage);
        client.notifyResponse(mqttConnAckMessage);
    }

}
