package org.smartboot.mqtt.common.listener;

import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttMessage;

import java.util.EventListener;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public interface MqttSessionListener<T extends AbstractSession> extends EventListener {
    default void onSessionCreate(T session) {
    }


    default void onMessageReceived(T session, MqttMessage mqttMessage) {
    }

    default void onMessageWrite(T session, MqttMessage mqttMessage) {
    }
}
