package org.smartboot.mqtt.broker.eventbus;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.messagebus.Message;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.eventbus.Subscriber;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * 将接收到的Publish消息推送至消息总线
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class MessageToMessageBusSubscriber implements Subscriber<EventObject<MqttPublishMessage>> {
    private final BrokerContext brokerContext;

    public MessageToMessageBusSubscriber(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @Override
    public void subscribe(EventType<EventObject<MqttPublishMessage>> eventType, EventObject<MqttPublishMessage> object) {
        Message message = brokerContext.getMessageBus().publish(object.getObject());
        //持久化消息
        brokerContext.getProviders().getPersistenceProvider().doSave(message);
        //推送至客户端
        brokerContext.batchPublish(message.getTopic());
    }
}
