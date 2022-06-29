package org.smartboot.mqtt.common.eventbus;

import org.smartboot.mqtt.common.AbstractSession;

/**
 * 通用事件模型
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventObject<T> {
    private final AbstractSession session;
    private final T object;

    private EventObject(AbstractSession session, T object) {
        this.session = session;
        this.object = object;
    }

    public static <T> EventObject<T> newEventObject(AbstractSession mqttSession, T object) {
        return new EventObject<>(mqttSession, object);
    }

    public AbstractSession getSession() {
        return session;
    }

    public T getObject() {
        return object;
    }
}
