package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.store.SubscriberConsumeOffset;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.util.MqttUtil;

/**
 * 客户端订阅消息
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SubscribeProcessor extends AuthorizedMqttProcessor<MqttSubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeProcessor.class);

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttSubscribeMessage mqttSubscribeMessage) {
        LOGGER.info("receive subscribe message:{}", mqttSubscribeMessage);

        //有效载荷包含一个返回码清单。每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        int[] qosArray = new int[mqttSubscribeMessage.getMqttSubscribePayload().topicSubscriptions().size()];
        int i = 0;
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getMqttSubscribePayload().topicSubscriptions()) {
            //如果服务端选择不支持包含通配符的主题过滤器，必须拒绝任何包含通配符过滤器的订阅请求。（PS：若需要支持通配符，请购买付费版）
            if (MqttUtil.containsTopicWildcards(mqttTopicSubscription.topicFilter())) {
                qosArray[i++] = MqttQoS.FAILURE.value();
            } else {
                qosArray[i++] = mqttTopicSubscription.qualityOfService().value();
                /*
                 * 如果主题过滤器不同于任何现存订阅的过滤器，服务端会创建一个新的订阅并发送所有匹配的保留消息。
                 */
                context.getProviders().getTopicFilterProvider().match(mqttTopicSubscription.topicFilter(), context, topic -> {
                    SubscriberConsumeOffset consumeOffset = new SubscriberConsumeOffset(topic, session, mqttTopicSubscription.qualityOfService());
                    session.subscribeTopic(consumeOffset);

                    //一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它必须被发送给这个订阅者
                    //todo
                });
            }
        }


        //订阅确认
        //允许服务端在发送 SUBACK 报文之前就开始发送与订阅匹配的 PUBLISH 报文
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage();
        mqttSubAckMessage.setPacketId(mqttSubscribeMessage.getPacketId());

        //有效载荷包含一个返回码清单。
        // 每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        mqttSubAckMessage.setMqttSubAckPayload(new MqttSubAckPayload(qosArray));
        session.write(mqttSubAckMessage);
    }
}
