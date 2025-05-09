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
import tech.smartboot.mqtt.common.message.MqttPubCompMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 4/22/23
 */
public class PubRelProcessor extends AuthorizedMqttProcessor<MqttPubRelMessage> {
    @Override
    public void process0(BrokerContextImpl context, MqttSessionImpl session, MqttPubRelMessage message) {
        //发送pubRel消息。
        //todo
        MqttPubQosVariableHeader qosVariableHeader;
        //todo
        byte code = 0;
        if (code != 0) {
            ReasonProperties properties = new ReasonProperties();
            qosVariableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
            qosVariableHeader.setReasonCode(code);
        } else {
            qosVariableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), null);
        }
        MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(qosVariableHeader);
        session.write(pubRelMessage, false);
        session.notifyPubComp(message.getVariableHeader().getPacketId());
    }
}
