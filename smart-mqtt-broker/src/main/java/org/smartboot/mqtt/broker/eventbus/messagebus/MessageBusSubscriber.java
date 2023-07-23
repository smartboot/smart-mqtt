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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.MqttSession;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusSubscriber.class);
    /**
     * 消息总线消费者
     */
    private final List<Consumer> messageBuses = new ArrayList<>();

    @Override
    public void consumer(Consumer consumer) {
        messageBuses.add(consumer);
    }

    @Override
    public void consumer(Consumer consumer, Predicate<Message> filter) {
        consumer((publishMessage) -> {
            if (filter.test(publishMessage)) {
                consumer.consume(publishMessage);
            }
        });
    }

    @Override
    public void consume(MqttSession mqttSession, MqttPublishMessage message) {
        Message persistenceMessage = new Message(mqttSession, message);
        boolean remove = false;
        for (Consumer messageConsumer : messageBuses) {
            try {
                if (messageConsumer.enable()) {
                    messageConsumer.consume(persistenceMessage);
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                LOGGER.info("messageBus conumse exception", throwable);
            }
        }
        if (remove) {
            messageBuses.removeIf(consumer -> !consumer.enable());
        }
    }
}
