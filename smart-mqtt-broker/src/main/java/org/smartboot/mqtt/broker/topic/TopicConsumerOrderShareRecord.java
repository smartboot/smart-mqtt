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

import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 顺序共享订阅
 */
class TopicConsumerOrderShareRecord extends AbstractConsumerRecord {
    private final ConcurrentLinkedQueue<TopicConsumerRecord> queue;

    private final AtomicBoolean semaphore = new AtomicBoolean(false);

    public TopicConsumerOrderShareRecord(BrokerTopic topic, TopicToken topicFilterToken, ConcurrentLinkedQueue<TopicConsumerRecord> queue) {
        super(topic, topicFilterToken, topic.getMessageQueue().getLatestOffset() + 1);
        this.queue = queue;
        topic.addSubscriber(this);
    }

    @Override
    public void pushToClient() {
        if (semaphore.compareAndSet(false, true)) {
            push0();
        }
    }

    public void push0() {
        int i = 10000;
        while (i-- > 0) {
            Message message = topic.getMessageQueue().get(nextConsumerOffset);
            if (message == null) {
                if (semaphore.compareAndSet(true, false)) {
                    topic.addSubscriber(this);
                    if (topic.getMessageQueue().get(nextConsumerOffset) != null && !queue.isEmpty()) {
                        topic.push();
                    }
                }
                return;
            }
            TopicConsumerRecord record = queue.poll();
            if (record == null) {
                if (semaphore.compareAndSet(true, false)) {
                    if (topic.getMessageQueue().get(nextConsumerOffset) != null && !queue.isEmpty()) {
                        topic.addSubscriber(this);
                        topic.push();
                    }
                }
                return;
            }

            MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(message.getPayload()).qos(record.getMqttQoS()).topicName(message.getTopic());
            if (record.getMqttSession().getMqttVersion() == MqttVersion.MQTT_5) {
                publishBuilder.publishProperties(new PublishProperties());
            }

            //Qos0直接发送
            if (record.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
                nextConsumerOffset++;
                record.getMqttSession().write(publishBuilder.build());
                queue.offer(record);
                continue;
            }

            CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = record.getMqttSession().getInflightQueue().offer(publishBuilder, () -> {
                queue.offer(record);
            });
            if (future != null) {
                nextConsumerOffset++;
                queue.offer(record);
            }
        }
        if (semaphore.compareAndSet(true, false)) {
            topic.addSubscriber(this);
            if (topic.getMessageQueue().get(nextConsumerOffset) != null && !queue.isEmpty()) {
                topic.push();
            }
        }
    }
}
