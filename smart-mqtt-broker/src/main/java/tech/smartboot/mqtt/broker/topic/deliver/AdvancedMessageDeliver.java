/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic.deliver;

import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.broker.TopicSubscription;
import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.PublishBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Qos1/2 Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class AdvancedMessageDeliver extends BaseMessageDeliver {

    private final AtomicBoolean semaphore = new AtomicBoolean(false);


    public AdvancedMessageDeliver(BrokerTopicImpl topic, MqttSessionImpl session, TopicSubscription topicSubscription, long nextConsumerOffset) {
        super(topic, session, topicSubscription, nextConsumerOffset);
        ValidateUtils.isTrue(topicSubscription.getMqttQoS() != MqttQoS.AT_MOST_ONCE, "invalid qos");
    }

    /**
     * 推送消息到客户端
     */
    public void run() {
        if (getMqttSession().isDisconnect() || !enable) {
            return;
        }
        if (semaphore.compareAndSet(false, true)) {
            push0();
            getMqttSession().flush();
        }
    }

    private void push0() {
        Message message = topic.getMessageQueue().get(nextConsumerOffset);
        //消息队列已消费至最新点位
        if (message == null) {
            if (semaphore.compareAndSet(true, false)) {
                topic.registerMessageDeliver(this);
                if (topic.getMessageQueue().get(nextConsumerOffset) != null) {
                    topic.push();
                }
            }
            return;
        }
        int available = getMqttSession().getInflightQueue().available();
        //当前连接的飞行窗口已满
        if (available == 0) {
            if (semaphore.compareAndSet(true, false)) {
                topic.registerMessageDeliver(this);
                if (getMqttSession().getInflightQueue().available() > 0) {
                    topic.push();
                }
            }
            return;
        }

        PublishBuilder publishBuilder = PublishBuilder.builder().payload(message.getPayload()).qos(getMqttQoS()).topic(message.getTopic());
        if (getMqttSession().getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }
        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = getMqttSession().getInflightQueue().offer(publishBuilder);
        if (future != null) {
            topic.getMessageQueue().commit(message.getOffset());
            nextConsumerOffset = message.getOffset() + 1;
            //如果存在共享订阅，则有可能出现available为1，但future为null的情况
            if (available == 1) {
                future.whenComplete((mqttPacketIdentifierMessage, throwable) -> push0());
            }
        }
        push0();
    }
}
