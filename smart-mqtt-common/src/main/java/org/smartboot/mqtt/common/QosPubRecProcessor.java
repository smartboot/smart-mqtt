package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;

/**
 * @author qinluo
 * @date 2022-04-12 14:13:42
 * @since 1.0.0
 */
public class QosPubRecProcessor implements QosCallbackProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QosPubRecProcessor.class);

    @Override
    public void process(QosCallbackController controller, QosCallback callback, MqttMessage message, AioSession session) {
        ValidateUtils.isTrue(message instanceof MqttPubRecMessage, "Uncorrected message in pubrec");
        LOGGER.info("Qos2 rec received : " + callback.getPacketId());

        callback.update();

        ValidateUtils.isTrue(callback.hasNextCallback(), "uncorrected callback object");
        callback.nextCallback();

        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0));
        pubRelMessage.setPacketId(callback.getPacketId());
        // write pubrel
        controller.write(pubRelMessage);
    }
}
