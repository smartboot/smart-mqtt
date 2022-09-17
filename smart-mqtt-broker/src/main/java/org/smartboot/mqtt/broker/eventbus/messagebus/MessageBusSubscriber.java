package org.smartboot.mqtt.broker.eventbus.messagebus;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.Message;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 将接收到的Publish消息推送至消息总线
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class MessageBusSubscriber implements MessageBus {
    private final BrokerContext brokerContext;

    public MessageBusSubscriber(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @Override
    public void subscribe(EventType<EventObject<MqttPublishMessage>> eventType, EventObject<MqttPublishMessage> object) {
        //进入到消息总线前要先确保BrokerTopic已创建
        BrokerTopic topic = brokerContext.getOrCreateTopic(object.getObject().getVariableHeader().getTopicName());
        Message message = new Message(object.getObject());
        brokerContext.getMessageBus().producer(message);
        //持久化消息
        brokerContext.getProviders().getPersistenceProvider().doSave(message);
        //推送至客户端
        brokerContext.batchPublish(topic);
    }

    private final List<Consumer<Message>> messageBuses = new ArrayList<>();

    @Override
    public void consumer(Consumer<Message> consumer) {
        consumer(consumer, message -> true);
    }

    @Override
    public void consumer(Consumer<Message> consumer, Predicate<Message> filter) {
        messageBuses.add(message -> {
            if (filter.test(message)) {
                consumer.accept(message);
            }
        });
    }

    @Override
    public void producer(Message message) {
        messageBuses.forEach(messageConsumer -> messageConsumer.accept(message));
    }
}
