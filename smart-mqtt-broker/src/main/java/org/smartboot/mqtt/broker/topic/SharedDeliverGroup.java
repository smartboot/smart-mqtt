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

import org.smartboot.mqtt.broker.topic.deliver.AbstractMessageDeliver;
import org.smartboot.mqtt.broker.topic.deliver.SharedOrderedMessageDeliver;
import org.smartboot.mqtt.plugin.spec.MqttSession;

class SharedDeliverGroup extends DeliverGroup {
    private final SharedOrderedMessageDeliver record;

    public SharedDeliverGroup(BrokerTopicImpl brokerTopic) {
        record = new SharedOrderedMessageDeliver(brokerTopic);
    }

    @Override
    public AbstractMessageDeliver getSubscriber(MqttSession session) {
        return record;
    }

    @Override
    public void addSubscriber(AbstractMessageDeliver subscriber) {
        super.addSubscriber(subscriber);
        record.getQueue().offer(subscriber);
    }

    @Override
    public AbstractMessageDeliver removeSubscriber(MqttSession session) {
        AbstractMessageDeliver consumerRecord = super.removeSubscriber(session);
        if (subscribers.isEmpty()) {
            record.disable();
            record.getTopic().removeShareGroup(consumerRecord.getTopicFilterToken().getTopicFilter());
        }
        return consumerRecord;
    }
}
