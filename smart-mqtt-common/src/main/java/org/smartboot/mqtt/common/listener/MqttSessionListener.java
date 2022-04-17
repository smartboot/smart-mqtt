package org.smartboot.mqtt.common.listener;

import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public interface MqttSessionListener {
    void onMessageReceived(AbstractSession mqttClient, MqttMessage mqttMessage);

    void onMessageWrite(AbstractSession session, MqttMessage mqttMessage);
}
