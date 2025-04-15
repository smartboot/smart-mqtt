/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 消息总线服务，接收由客户端发送过来的消息，并通过总线投递给订阅者进行消费
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public class MessageBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBus.class);
    /**
     * 消息总线消费者
     */
    private final List<Consumer> messageBuses = new ArrayList<>();
    private final BrokerContext brokerContext;

    public MessageBus(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    /**
     * 订阅消息总线消费者
     */
    public void consumer(Consumer consumer) {
        messageBuses.add(consumer);
    }

    /**
     * 订阅消息总线消费者
     *
     * @param consumer 消费者
     * @param filter   消费条件
     */
    public void consumer(Consumer consumer, Predicate<Message> filter) {
        consumer((session, publishMessage) -> {
            if (filter.test(publishMessage)) {
                consumer.consume(session, publishMessage);
            }
        });
    }

    /**
     * 发布消息至总线触发消费
     */
    public void publish(MqttSession mqttSession, MqttPublishMessage publishMessage) {
        Message message = new Message(publishMessage, brokerContext.getOrCreateTopic(publishMessage.getVariableHeader().getTopicName()));
        boolean remove = false;
        for (Consumer messageConsumer : messageBuses) {
            try {
                if (messageConsumer.enable()) {
                    messageConsumer.consume(mqttSession, message);
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                LOGGER.info("messageBus consume exception", throwable);
            }
        }
        if (remove) {
            messageBuses.removeIf(consumer -> !consumer.enable());
        }
    }
}
