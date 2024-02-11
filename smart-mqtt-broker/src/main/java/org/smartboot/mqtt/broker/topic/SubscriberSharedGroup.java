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

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.TopicToken;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SubscriberSharedGroup extends SubscriberGroup {
    private final TopicConsumerOrderShareRecord record;
    private final ConcurrentLinkedQueue<TopicConsumerRecord> queue = new ConcurrentLinkedQueue<>();

    public SubscriberSharedGroup(TopicToken topicFilterToken, BrokerTopic brokerTopic) {
        record = new TopicConsumerOrderShareRecord(brokerTopic, topicFilterToken, queue);
    }

    @Override
    public void addSubscriber(TopicConsumerRecord subscriber) {
        super.addSubscriber(subscriber);
        queue.add(subscriber);
    }

    @Override
    public AbstractConsumerRecord getSubscriber(MqttSession session) {
        return record;
    }

    @Override
    public TopicConsumerRecord removeSubscriber(MqttSession session) {
        TopicConsumerRecord consumerRecord = super.removeSubscriber(session);
        if (subscribers.isEmpty()) {
            record.disable();
            record.topic.removeShareGroup(record.getTopicFilterToken().getTopicFilter());
        }
        return consumerRecord;
    }
}
