package org.smartboot.mqtt.broker.eventbus;

import org.smartboot.mqtt.broker.MqttSession;

/**
 * 通用事件模型
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventObject<T> {
    private final MqttSession session;
    private final T object;

    private EventObject(MqttSession session, T object) {
        this.session = session;
        this.object = object;
    }

    public static <T> EventObject<T> newEventObject(MqttSession mqttSession, T object) {
        return new EventObject<>(mqttSession, object);
    }

    public MqttSession getSession() {
        return session;
    }

    public T getObject() {
        return object;
    }
}
