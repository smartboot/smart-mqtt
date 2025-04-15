/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import org.smartboot.mqtt.plugin.spec.bus.EventBus;
import org.smartboot.mqtt.plugin.spec.bus.EventBusSubscriber;
import org.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class EventBusImpl implements EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusImpl.class);

    private final Map<EventType, List<EventBusSubscriber>> map = new ConcurrentHashMap<>();

    public static List<EventBusSubscriber> WRITE_MESSAGE_SUBSCRIBER_LIST = new CopyOnWriteArrayList<>();

    public static List<EventBusSubscriber> RECEIVE_MESSAGE_SUBSCRIBER_LIST = new CopyOnWriteArrayList<>();


    public <T> void subscribe(EventType<T> type, EventBusSubscriber<T> subscriber) {
        LOGGER.debug("subscribe eventbus, type: {} ,subscriber: {}", type, subscriber);
        if (type.isOnce() && !(subscriber instanceof DisposableEventBusSubscriber)) {
            getSubscribers(type).add(new DisposableEventBusSubscriber<T>() {
                @Override
                public void subscribe(EventType<T> eventType, T object) {
                    subscriber.subscribe(eventType, object);
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
        List<EventBusSubscriber> list = getSubscribers(eventType);
        publish(eventType, object, list);
    }

    private List<EventBusSubscriber> getSubscribers(EventType eventType) {
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
