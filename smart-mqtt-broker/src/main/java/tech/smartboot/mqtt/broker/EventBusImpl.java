/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import tech.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import tech.smartboot.mqtt.plugin.spec.bus.EventBus;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
class EventBusImpl implements EventBus {

    private final Map<EventType, List<EventBusConsumer>> map = new ConcurrentHashMap<>();

    public static List<EventBusConsumer> WRITE_MESSAGE_SUBSCRIBER_LIST = new CopyOnWriteArrayList<>();

    public static List<EventBusConsumer> RECEIVE_MESSAGE_SUBSCRIBER_LIST = new CopyOnWriteArrayList<>();


    public <T> void subscribe(EventType<T> type, EventBusConsumer<T> subscriber) {
        if (type.isOnce() && !(subscriber instanceof DisposableEventBusSubscriber)) {
            getSubscribers(type).add(new DisposableEventBusSubscriber<T>() {
                @Override
                public void consumer(EventType<T> eventType, T object) {
                    subscriber.consumer(eventType, object);
                }
            });
        } else {
            getSubscribers(type).add(subscriber);
        }
    }


    /**
     * 发布消息至总线
     */
    public <T> void publish(EventType<T> eventType, T object) {
        List<EventBusConsumer> list = getSubscribers(eventType);
        publish(eventType, object, list);
    }

    private List<EventBusConsumer> getSubscribers(EventType eventType) {
        return map.computeIfAbsent(eventType, type -> {
            if (type == EventType.WRITE_MESSAGE) {
                return WRITE_MESSAGE_SUBSCRIBER_LIST;
            }
            if (type == EventType.RECEIVE_MESSAGE) {
                return RECEIVE_MESSAGE_SUBSCRIBER_LIST;
            }
            return new CopyOnWriteArrayList<>();
        });
    }
}
