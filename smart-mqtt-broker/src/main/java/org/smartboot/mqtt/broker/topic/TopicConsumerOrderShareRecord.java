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
import java.util.concurrent.Semaphore;

/**
 * 顺序共享订阅
 */
class TopicConsumerOrderShareRecord extends AbstractConsumerRecord {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicConsumerOrderShareRecord.class);
    private final ConcurrentLinkedQueue<TopicConsumerRecord> queue = new ConcurrentLinkedQueue<>();

    private final Semaphore semaphore = new Semaphore(1);

    public TopicConsumerOrderShareRecord(BrokerTopic topic, TopicToken topicFilterToken) {
        super(topic, topicFilterToken, topic.getMessageQueue().getLatestOffset() + 1);
        topic.addSubscriber(this);
    }

    public ConcurrentLinkedQueue<TopicConsumerRecord> getQueue() {
        return queue;
    }

    @Override
    public void pushToClient() {
        if (semaphore.tryAcquire()) {
            try {
                push0();
            } finally {
                semaphore.release();
            }
            topic.addSubscriber(this);
            if (topic.getMessageQueue().get(nextConsumerOffset) != null && !queue.isEmpty()) {
                //触发下一轮推送
                topic.getVersion().incrementAndGet();
            }
        }
    }

    private void push0() {
        int i = 10000;
        while (i-- > 0) {
            Message message = topic.getMessageQueue().get(nextConsumerOffset);
            if (message == null) {
                return;
            }
            TopicConsumerRecord record = queue.poll();
            //共享订阅列表无可用通道
            if (record == null) {
                return;
            }

            if (!record.enable || record.getMqttSession().isDisconnect()) {
                continue;
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
                LOGGER.debug("publish share subscribe:{} to {}", topicFilterToken.getTopicFilter(), record.getMqttSession().getClientId());
                continue;
            }

            CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = record.getMqttSession().getInflightQueue().offer(publishBuilder, () -> {
                queue.offer(record);
            });
            if (future != null) {
                nextConsumerOffset++;
                record.getMqttSession().flush();
                queue.offer(record);
            }
        }
    }
}
