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

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import tech.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import tech.smartboot.mqtt.common.message.payload.Mqtt5UnsubAckPayload;
import tech.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

/**
 * 客户端发送 UNSUBSCRIBE 报文给服务端，用于取消订阅主题。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class UnSubscribeProcessor extends AuthorizedMqttProcessor<MqttUnsubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnSubscribeProcessor.class);

    @Override
    public void process0(BrokerContextImpl context, MqttSessionImpl session, MqttUnsubscribeMessage unsubscribeMessage) {
        LOGGER.info("receive unsubscribe message:{}", unsubscribeMessage);
        context.getEventBus().publish(EventType.UNSUBSCRIBE_ACCEPT, EventObject.newEventObject(session, unsubscribeMessage));

        //TODO
        //如果服务端删除了一个订阅：
        //  它必须停止分发任何新消息给这个客户端 [MQTT-3.10.4-2]。
        //  它必须完成分发任何已经开始往客户端发送的 QoS 1 和 QoS 2 的消息 [MQTT-3.10.4-3]。
        //  它可以继续发送任何现存的准备分发给客户端的缓存消息。
        unsubscribeMessage.getMqttUnsubscribePayload().topics()
                .forEach(session::unsubscribe);

        //当前是否存在符合条件的通配符匹配
        session.resubscribe();

        //取消订阅确认
        MqttReasonVariableHeader variableHeader;
        Mqtt5UnsubAckPayload payload;
        //todo
        if (unsubscribeMessage.getVersion() == MqttVersion.MQTT_5) {
            ReasonProperties properties = new ReasonProperties();
            variableHeader = new MqttReasonVariableHeader(unsubscribeMessage.getVariableHeader().getPacketId(), properties);
            //todo 暂时默认都成功
            payload = new Mqtt5UnsubAckPayload(new byte[unsubscribeMessage.getMqttUnsubscribePayload().topics().size()]);
        } else {
            variableHeader = new MqttReasonVariableHeader(unsubscribeMessage.getVariableHeader().getPacketId(), null);
            payload = new Mqtt5UnsubAckPayload(new byte[0]);
        }
        MqttUnsubAckMessage mqttSubAckMessage = new MqttUnsubAckMessage(variableHeader, payload);
        session.write(mqttSubAckMessage);
    }
}
