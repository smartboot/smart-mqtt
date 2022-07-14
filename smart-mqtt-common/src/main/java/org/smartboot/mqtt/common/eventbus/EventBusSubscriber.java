package org.smartboot.mqtt.common.eventbus;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public interface EventBusSubscriber<T> {
    void subscribe(EventType<T> eventType, T object);

    default boolean enable() {
        return true;
    }
}
