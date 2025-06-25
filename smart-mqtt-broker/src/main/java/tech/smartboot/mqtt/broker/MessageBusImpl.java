/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBus;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息总线服务，接收由客户端发送过来的消息，并通过总线投递给订阅者进行消费
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
class MessageBusImpl implements MessageBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusImpl.class);
    /**
     * 消息总线消费者
     */
    private final List<MessageBusConsumer> messageBuses = new ArrayList<>();
    private final BrokerContext brokerContext;

    public MessageBusImpl(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    /**
     * 订阅消息总线消费者
     */
    public void consumer(MessageBusConsumer consumer) {
        messageBuses.add(consumer);
    }

    @Override
    public void publish(MqttSession mqttSession, Message message) {
        boolean remove = false;
        for (MessageBusConsumer messageConsumer : messageBuses) {
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
