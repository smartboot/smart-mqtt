package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public class MqttPingRespProcessor implements MqttProcessor<MqttPingRespMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPingRespProcessor.class);

    @Override
    public void process(MqttClient mqttClient, MqttPingRespMessage message) {
//        LOGGER.info("receive ping response");
    }
}
