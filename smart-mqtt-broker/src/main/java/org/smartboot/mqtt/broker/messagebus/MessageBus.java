package org.smartboot.mqtt.broker.messagebus;

import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * 消息总线服务，接收由客户端发送过来的消息，并通过总线投递给订阅者进行消费
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public interface MessageBus {

    void subscribe(Subscriber subscriber);

    void subscribe(Subscriber subscriber, MessageFilter filter);

    /**
     * 发布消息至总线
     */
    Message publish(MqttPublishMessage storedMessage);

}
