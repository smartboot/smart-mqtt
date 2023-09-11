/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.payload.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

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
        int[] qosArray = new int[mqttSubscribeMessage.getPayload().getTopicSubscriptions().size()];
        int i = 0;
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getPayload().getTopicSubscriptions()) {
            qosArray[i++] = session.subscribe(mqttTopicSubscription.getTopicFilter(), mqttTopicSubscription.getQualityOfService()).value();
            context.getEventBus().publish(ServerEventType.SUBSCRIBE_ACCEPT, EventObject.newEventObject(session, mqttTopicSubscription));
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
    }
}
