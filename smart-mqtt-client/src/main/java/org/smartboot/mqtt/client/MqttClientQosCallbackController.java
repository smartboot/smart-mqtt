package org.smartboot.mqtt.client;

import org.smartboot.mqtt.common.QosCallbackController;
import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author qinluo
 * @date 2022-04-12 14:19:17
 * @since 1.0.0
 */
public class MqttClientQosCallbackController extends QosCallbackController {

    private MqttClient client;

    public MqttClientQosCallbackController(MqttClient client) {
        this.client = client;
    }

    @Override
    public void write(MqttMessage message) {
        client.write(message);
    }
}
