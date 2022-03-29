package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.common.Topic;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttSubAckMessage;
import org.smartboot.socket.mqtt.message.MqttSubAckPayload;
import org.smartboot.socket.mqtt.message.MqttSubscribeMessage;
import org.smartboot.socket.mqtt.message.MqttTopicSubscription;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.store.SubscriberConsumeOffset;

/**
 * 客户端订阅消息
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SubscribeProcessor implements MqttProcessor<MqttSubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeProcessor.class);

    @Override
    public void process(MqttContext context, MqttSession session, MqttSubscribeMessage mqttSubscribeMessage) {
        LOGGER.info("receive subscribe message:{}", mqttSubscribeMessage);

        //订阅Topic
        int[] qosArray = new int[mqttSubscribeMessage.getMqttSubscribePayload().topicSubscriptions().size()];
        int i = 0;
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getMqttSubscribePayload().topicSubscriptions()) {
            qosArray[i++] = mqttTopicSubscription.qualityOfService().value();
            /*
             * 如果主题过滤器不同于任何现存订阅的过滤器，服务端会创建一个新的订阅并发送所有匹配的保留消息。
             */
            Topic topic = context.getOrCreateTopic(mqttTopicSubscription.topicName());
            SubscriberConsumeOffset consumeOffset = new SubscriberConsumeOffset(topic, session, mqttTopicSubscription.qualityOfService());
            session.subscribeTopic(consumeOffset);
            context.getTopicListener().notify(consumeOffset);
        }


        //订阅确认
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0));
        mqttSubAckMessage.setPacketId(mqttSubscribeMessage.getPacketId());

        //有效载荷包含一个返回码清单。
        // 每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        mqttSubAckMessage.setMqttSubAckPayload(new MqttSubAckPayload(qosArray));
        session.write(mqttSubAckMessage);
    }
}
