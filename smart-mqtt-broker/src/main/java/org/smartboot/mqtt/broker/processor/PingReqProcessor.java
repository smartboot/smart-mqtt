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

import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.MqttSession;

/**
 * 心跳请求处理
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PingReqProcessor extends AuthorizedMqttProcessor<MqttPingReqMessage> {
    private static final MqttPingRespMessage RESP_MESSAGE = new MqttPingRespMessage();

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPingReqMessage msg) {
        session.write(RESP_MESSAGE);
    }
}
