/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.bus;

import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.function.Predicate;

/**
 * 消息总线服务，接收由客户端发送过来的消息，并通过总线投递给订阅者进行消费
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public interface MessageBus {

    /**
     * 订阅消息总线消费者
     */
    void consumer(MessageBusConsumer consumer);

    /**
     * 订阅消息总线消费者
     *
     * @param consumer 消费者
     * @param filter   消费条件
     */
    default void consumer(MessageBusConsumer consumer, Predicate<Message> filter) {
        consumer((session, publishMessage) -> {
            if (filter.test(publishMessage)) {
                consumer.consume(session, publishMessage);
            }
        });
    }

    void publish(MqttSession mqttSession, Message publishMessage);
}
