package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.transport.AioSession;

/**
 * @author qinluo
 * @date 2022-04-12 13:38:25
 * @since 1.0.0
 */
public interface QosCallbackProcessor {

    void process(QosCallbackController controller, QosCallback callback, MqttMessage message, AioSession session);
}
