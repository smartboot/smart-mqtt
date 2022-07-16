package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;

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
        //有效载荷包含一个返回码清单。每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        int[] qosArray = new int[mqttSubscribeMessage.getMqttSubscribePayload().getTopicSubscriptions().size()];
        int i = 0;
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getMqttSubscribePayload().getTopicSubscriptions()) {
            session.subscribe(mqttTopicSubscription.getTopicFilter(), mqttTopicSubscription.getQualityOfService());
            qosArray[i++] = mqttTopicSubscription.getQualityOfService().value();
        }


        //订阅确认
        //允许服务端在发送 SUBACK 报文之前就开始发送与订阅匹配的 PUBLISH 报文
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(mqttSubscribeMessage.getVariableHeader().getPacketId());

        //有效载荷包含一个返回码清单。
        // 每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        mqttSubAckMessage.setMqttSubAckPayload(new MqttSubAckPayload(qosArray));
        session.write(mqttSubAckMessage);
    }
}
