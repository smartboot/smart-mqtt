/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic.deliver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.MqttSessionImpl;
import org.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.plugin.spec.Message;
import org.smartboot.mqtt.plugin.spec.PublishBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * 顺序共享订阅
 */
public class SharedOrderedMessageDeliver extends AbstractMessageDeliver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedOrderedMessageDeliver.class);
    /**
     * 共享订阅者队列
     */
    private final ConcurrentLinkedQueue<AbstractMessageDeliver> queue = new ConcurrentLinkedQueue<>();

    private final Semaphore semaphore = new Semaphore(1);

    public SharedOrderedMessageDeliver(BrokerTopicImpl topic) {
        super(topic, null, null, topic.getMessageQueue().getLatestOffset() + 1);
        //将共享订阅者加入 BrokerTopic 的推送列表中
        topic.addSubscriber(this);
    }

    public ConcurrentLinkedQueue<AbstractMessageDeliver> getQueue() {
        return queue;
    }

    @Override
    public MqttSessionImpl getMqttSession() {
        throw new UnsupportedOperationException();
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
                topic.addVersion();
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
            AbstractMessageDeliver record = queue.poll();
            //共享订阅列表无可用通道
            if (record == null) {
                return;
            }

            if (!record.isEnable() || record.getMqttSession().isDisconnect()) {
                continue;
            }
            PublishBuilder publishBuilder = PublishBuilder.builder().payload(message.getPayload()).qos(record.getMqttQoS()).topic(message.getTopic());
            if (record.getMqttSession().getMqttVersion() == MqttVersion.MQTT_5) {
                publishBuilder.publishProperties(new PublishProperties());
            }

            //Qos0直接发送
            if (record.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
                topic.getMessageQueue().commit(nextConsumerOffset++);
                record.getMqttSession().write(publishBuilder.build());
                queue.offer(record);
                LOGGER.debug("publish share subscribe:{} to {}", record.getTopicFilterToken().getTopicFilter(), record.getMqttSession().getClientId());
                continue;
            }


            //若future为null，则说明该连接的飞行窗口已满，需要待其释放出空间后再重新投递至共享订阅列表，否则容易造成死循环
            CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = record.getMqttSession().getInflightQueue().offer(publishBuilder, () -> {
                queue.offer(record);
            });
            if (future != null) {
                topic.getMessageQueue().commit(nextConsumerOffset++);
                record.getMqttSession().flush();
                queue.offer(record);
            }
        }
    }
}
