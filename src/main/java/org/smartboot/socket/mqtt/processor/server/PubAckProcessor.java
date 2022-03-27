package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

/**
 * PUBACK 报文是对 QoS 1 等级的 PUBLISH 报文的响应
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PubAckProcessor implements MqttProcessor<MqttPubAckMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubAckProcessor.class);

    @Override
    public void process(MqttContext context, MqttSession session, MqttPubAckMessage pubAckMessage) {
        LOGGER.info("receive pubAck message:{}", pubAckMessage);
        session.getQosTask(pubAckMessage.getPacketId()).done();
    }
}
