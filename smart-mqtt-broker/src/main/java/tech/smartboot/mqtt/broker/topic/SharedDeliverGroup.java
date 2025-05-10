/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic;

import tech.smartboot.mqtt.plugin.spec.MessageDeliver;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

class SharedDeliverGroup extends DeliverGroup {
    private final SharedOrderedMessageDeliver record;

    public SharedDeliverGroup(BrokerTopicImpl brokerTopic) {
        record = new SharedOrderedMessageDeliver(brokerTopic);
    }

    @Override
    public MessageDeliver getMessageDeliver(MqttSession session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMessageDeliver(BaseMessageDeliver subscriber) {
        super.addMessageDeliver(subscriber);
        record.getQueue().offer(subscriber);
    }

    @Override
    public BaseMessageDeliver removeMessageDeliver(MqttSession session) {
        BaseMessageDeliver messageDeliver = super.removeMessageDeliver(session);
        if (subscribers.isEmpty()) {
            record.disable();
            record.getTopic().removeShareGroup(messageDeliver.getSubscribeRelation().getTopicFilter());
        }
        return messageDeliver;
    }

    public boolean isShared() {
        return true;
    }
}
