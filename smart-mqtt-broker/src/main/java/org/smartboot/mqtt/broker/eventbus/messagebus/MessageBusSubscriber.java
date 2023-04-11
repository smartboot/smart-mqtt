/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus.messagebus;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.messagebus.consumer.Consumer;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 将接收到的Publish消息推送至消息总线
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class MessageBusSubscriber implements MessageBus {

    /**
     * 消息总线消费者
     */
    private final List<Consumer> messageBuses = new ArrayList<>();

    @Override
    public void consumer(Consumer consumer) {
        messageBuses.add(consumer);
    }

    @Override
    public void consumer(Consumer consumer, Predicate<MqttPublishMessage> filter) {
        consumer((brokerContext, publishMessage) -> {
            if (filter.test(publishMessage)) {
                consumer.consume(brokerContext, publishMessage);
            }
        });
    }

    @Override
    public void consume(BrokerContext brokerContext, MqttPublishMessage message) {
        messageBuses.forEach(messageConsumer -> messageConsumer.consume(brokerContext, message));
    }
}
