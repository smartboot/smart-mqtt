/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.MqttSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriberGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberGroup.class);

    protected final Map<MqttSession, TopicConsumerRecord> subscribers = new ConcurrentHashMap<>();

    public AbstractConsumerRecord getSubscriber(MqttSession session) {
        return subscribers.get(session);
    }

    public AbstractConsumerRecord removeSubscriber(MqttSession session) {
        return subscribers.remove(session);
    }

    public void addSubscriber(TopicConsumerRecord subscriber) {
        subscribers.put(subscriber.getMqttSession(), subscriber);
    }
}
