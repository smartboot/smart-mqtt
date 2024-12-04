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
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Qos1/2 Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class TopicQosConsumerRecord extends TopicConsumerRecord {

    private final AtomicBoolean semaphore = new AtomicBoolean(false);


    public TopicQosConsumerRecord(BrokerTopic topic, MqttSession session, TopicSubscriber topicSubscriber, long nextConsumerOffset) {
        super(topic, session, topicSubscriber, nextConsumerOffset);
        ValidateUtils.isTrue(topicSubscriber.getMqttQoS() != MqttQoS.AT_MOST_ONCE, "invalid qos");
    }

    /**
     * 推送消息到客户端
     */
    public void pushToClient() {
        if (mqttSession.isDisconnect() || !enable) {
            return;
        }
        if (semaphore.compareAndSet(false, true)) {
            push0();
            mqttSession.flush();
        }
    }

    private void push0() {
        Message message = topic.getMessageQueue().get(nextConsumerOffset);
        //消息队列已消费至最新点位
        if (message == null) {
            if (semaphore.compareAndSet(true, false)) {
                topic.addSubscriber(this);
                if (topic.getMessageQueue().get(nextConsumerOffset) != null) {
                    topic.push();
                }
            }
            return;
        }
        int available = mqttSession.getInflightQueue().available();
        //当前连接的飞行窗口已满
        if (mqttSession.getInflightQueue().available() == 0) {
            if (semaphore.compareAndSet(true, false)) {
                topic.addSubscriber(this);
                if (mqttSession.getInflightQueue().available() > 0) {
                    topic.push();
                }
            }
            return;
        }

        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(message.getPayload()).qos(mqttQoS).topic(message.getTopicBytes());
        if (mqttSession.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }
        topic.getMessageQueue().commit(message.getOffset());
        nextConsumerOffset = message.getOffset() + 1;

        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = mqttSession.getInflightQueue().offer(publishBuilder);
        if (available == 1) {
            future.whenComplete((mqttPacketIdentifierMessage, throwable) -> push0());
        } else {
            push0();
        }
    }
}
