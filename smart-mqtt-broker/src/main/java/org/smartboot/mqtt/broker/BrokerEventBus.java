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
import org.smartboot.mqtt.broker.eventbus.EventBus;
import org.smartboot.mqtt.broker.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.broker.eventbus.EventType;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class BrokerEventBus implements EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerEventBus.class);


    @Override
    public <T> void subscribe(EventType<T> type, EventBusSubscriber<T> subscriber) {
        LOGGER.debug("subscribe eventbus, type: {} ,subscriber: {}", type, subscriber);
        type.subscribe(subscriber);
    }

    @Override
    public <T> void subscribe(List<EventType<T>> types, EventBusSubscriber<T> subscriber) {
        for (EventType<T> eventType : types) {
            subscribe(eventType, subscriber);
        }
    }


    @Override
    public <T> void publish(EventType<T> eventType, T object) {
        eventType.publish(object);
    }
}
