package org.smartboot.mqtt.common.eventbus;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public interface EventBus {

    <T> void subscribe(EventType<T> type, Subscriber<T> subscriber);

    <T> void subscribe(List<EventType<T>> types, Subscriber<T> subscriber);


    /**
     * 发布消息至总线
     */
    <T> void publish(EventType<T> eventType, T object);
}
