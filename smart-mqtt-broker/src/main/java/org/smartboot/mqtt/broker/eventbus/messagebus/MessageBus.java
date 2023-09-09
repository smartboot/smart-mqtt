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

import org.smartboot.mqtt.broker.eventbus.messagebus.consumer.Consumer;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

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
    void consumer(Consumer consumer);

    /**
     * 订阅消息总线消费者
     *
     * @param consumer  消费者
     * @param predicate 消费条件
     */
    void consumer(Consumer consumer, Predicate<Message> predicate);

    /**
     * 发布消息至总线触发消费
     */
    void consume(AbstractSession mqttSession, MqttPublishMessage storedMessage);

}
