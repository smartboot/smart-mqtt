package org.smartboot.mqtt.common.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventBusImpl implements EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusImpl.class);
    private final List<EventBusSubscriber>[] lists;

    public EventBusImpl(List<EventType<?>> supportTypes) {
        lists = new List[supportTypes.size()];
        for (EventType<?> eventTypeEnum : supportTypes) {
            lists[eventTypeEnum.getIndex()] = new ArrayList<>();
        }
    }

    @Override
    public <T> void subscribe(EventType<T> type, EventBusSubscriber<T> subscriber) {
        lists[type.getIndex()].add(subscriber);
    }

    @Override
    public <T> void subscribe(List<EventType<T>> types, EventBusSubscriber<T> subscriber) {
        for (EventType<T> eventType : types) {
            subscribe(eventType, subscriber);
        }
    }


    @Override
    public <T> void publish(EventType<T> eventType, T object) {
        for (EventBusSubscriber<T> subscriber : lists[eventType.getIndex()]) {
            try {
                subscriber.subscribe(eventType, object);
            } catch (Throwable throwable) {
                LOGGER.error("", throwable);
            }
        }
    }
}
