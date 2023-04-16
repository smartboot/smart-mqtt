/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
            lists[eventTypeEnum.getIndex()] = new CopyOnWriteArrayList<>();
        }
    }

    @Override
    public <T> void subscribe(EventType<T> type, EventBusSubscriber<T> subscriber) {
        LOGGER.debug("subscribe eventbus, type: {} ,subscriber: {}", type, subscriber);
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
        List<EventBusSubscriber> list = lists[eventType.getIndex()];
        boolean remove = false;
        for (EventBusSubscriber<T> subscriber : list) {
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
}
