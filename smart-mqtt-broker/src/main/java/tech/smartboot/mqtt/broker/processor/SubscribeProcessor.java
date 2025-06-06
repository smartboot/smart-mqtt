/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.processor;

import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttSubAckMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttTopicSubscription;
import tech.smartboot.mqtt.common.message.payload.MqttSubAckPayload;
import tech.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端订阅消息
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SubscribeProcessor extends AuthorizedMqttProcessor<MqttSubscribeMessage> {

    @Override
    public void process0(BrokerContextImpl context, MqttSessionImpl session, MqttSubscribeMessage mqttSubscribeMessage) {
        //有效载荷包含一个返回码清单。每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        int[] qosArray = new int[mqttSubscribeMessage.getPayload().getTopicSubscriptions().size()];
        int i = 0;
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getPayload().getTopicSubscriptions()) {
            if (context.getProviders().getSubscribeProvider().subscribeTopic(mqttTopicSubscription.getTopicFilter(), session)) {
                subscriptions.add(mqttTopicSubscription);
                qosArray[i++] = mqttTopicSubscription.getQualityOfService().value();
            } else {
                qosArray[i++] = MqttQoS.FAILURE.value();
            }
        }


        //订阅确认
        //允许服务端在发送 SUBACK 报文之前就开始发送与订阅匹配的 PUBLISH 报文
        //todo
        ReasonProperties properties = null;
        if (mqttSubscribeMessage.getVersion() == MqttVersion.MQTT_5) {
            properties = new ReasonProperties();
        }
        MqttReasonVariableHeader variableHeader = new MqttReasonVariableHeader(mqttSubscribeMessage.getVariableHeader().getPacketId(), properties);

        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(variableHeader);

        //有效载荷包含一个返回码清单。
        // 每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        mqttSubAckMessage.setPayload(new MqttSubAckPayload(qosArray));
        session.write(mqttSubAckMessage, false);

        subscriptions.forEach(mqttTopicSubscription -> {
            session.subscribe(mqttTopicSubscription.getTopicFilter(), mqttTopicSubscription.getQualityOfService());
            context.getEventBus().publish(EventType.SUBSCRIBE_ACCEPT, EventObject.newEventObject(session, mqttTopicSubscription));
        });
    }
}
