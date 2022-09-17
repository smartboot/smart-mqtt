package org.smartboot.mqtt.broker.eventbus.messagebus;

import org.smartboot.mqtt.broker.Message;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 消息总线服务，接收由客户端发送过来的消息，并通过总线投递给订阅者进行消费
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public interface MessageBus extends EventBusSubscriber<EventObject<MqttPublishMessage>> {

    /**
     * 订阅消息总线消费者
     */
    void consumer(Consumer<Message> consumer);

    /**
     * 订阅消息总线消费者
     *
     * @param consumer  消费者
     * @param predicate 消费条件
     */
    void consumer(Consumer<Message> consumer, Predicate<Message> predicate);

    /**
     * 发布消息至总线
     */
    void producer(Message storedMessage);

}
