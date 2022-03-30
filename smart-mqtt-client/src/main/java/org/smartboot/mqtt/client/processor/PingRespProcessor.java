package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;

/**
 * 心跳请求处理
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PingRespProcessor implements MqttProcessor<MqttPingRespMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingRespProcessor.class);

    @Override
    public void process(MqttClientSession session, MqttPingRespMessage mqttPingRespMessage) {
//        MqttPingRespMessage mqttPingRespMessage = new MqttPingRespMessage();
//        session.write(mqttPingRespMessage);
//        LOGGER.info("response ping message:{}", mqttPingRespMessage);
    }


}
