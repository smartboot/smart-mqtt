package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;

/**
 * @author qinluo
 * @date 2022-04-12 13:56:27
 * @since 1.0.0
 */
public class QosPubAckProcessor implements QosCallbackProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QosPubAckProcessor.class);

    @Override
    public void process(QosCallbackController qosCallbackController, QosCallback callback, MqttMessage message, AioSession session) {
        ValidateUtils.isTrue(message instanceof MqttPubAckMessage, "Uncorrected message in ack");
        LOGGER.info("Qos1 message send success : " + callback.getPacketId());
        qosCallbackController.remove(callback.getClientId() + "_" + callback.getPacketId());
    }
}
