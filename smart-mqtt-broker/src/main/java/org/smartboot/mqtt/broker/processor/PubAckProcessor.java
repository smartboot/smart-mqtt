package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.store.StoredMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;

/**
 * PUBACK 报文是对 QoS 1 等级的 PUBLISH 报文的响应
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PubAckProcessor extends AuthorizedMqttProcessor<MqttPubAckMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubAckProcessor.class);

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPubAckMessage pubAckMessage) {
        LOGGER.info("receive pubAck message:{}", pubAckMessage);
        StoredMessage storedMessage = session.pollInFightMessage(pubAckMessage.getPacketId());
        LOGGER.info("pubAck message:{}", storedMessage);
//        session.getQosTask(pubAckMessage.getPacketId()).done();
    }
}
