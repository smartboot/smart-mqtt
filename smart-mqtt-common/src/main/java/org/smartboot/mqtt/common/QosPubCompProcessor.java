package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;

/**
 * @author qinluo
 * @date 2022-04-12 14:13:42
 * @since 1.0.0
 */
public class QosPubCompProcessor implements QosCallbackProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QosPubCompProcessor.class);

    @Override
    public void process(QosCallbackController controller, QosCallback callback, MqttMessage message, AioSession session) {
        ValidateUtils.isTrue(message instanceof MqttPubCompMessage, "Uncorrected message in comp");
        LOGGER.info("Qos2 comp received : " + callback.getPacketId());

        callback.update();
        controller.remove(callback.getClientId() + "_" + callback.getPacketId());
    }
}
