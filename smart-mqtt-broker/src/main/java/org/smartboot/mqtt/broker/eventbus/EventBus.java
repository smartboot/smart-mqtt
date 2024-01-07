/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private final Map<EventType, List<EventBusSubscriber>> map = new ConcurrentHashMap<>();

    public <T> void subscribe(EventType<T> type, EventBusSubscriber<T> subscriber) {
        LOGGER.debug("subscribe eventbus, type: {} ,subscriber: {}", type, subscriber);
        getSubscribers(type).add(subscriber);
    }

    public <T> void subscribe(List<EventType<T>> types, EventBusSubscriber<T> subscriber) {
        for (EventType<T> eventType : types) {
            subscribe(eventType, subscriber);
        }
    }

    /**
     * 发布消息至总线
     */
    public <T> void publish(EventType<T> eventType, T object) {
        List<EventBusSubscriber> list = getSubscribers(eventType);
        boolean remove = false;
        for (EventBusSubscriber subscriber : list) {
            try {
                if (subscriber.enable()) {
                    subscriber.subscribe(eventType, object);
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                LOGGER.error("publish event error", throwable);
            }
        }
        if (remove) {
            list.removeIf(eventBusSubscriber -> !eventBusSubscriber.enable());
        }
    }

    private List<EventBusSubscriber> getSubscribers(EventType eventType) {
        return map.computeIfAbsent(eventType, eventType1 -> new CopyOnWriteArrayList<>());
    }
}
