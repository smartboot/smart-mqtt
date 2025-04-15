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

import org.smartboot.mqtt.broker.BrokerContextImpl;
import org.smartboot.mqtt.broker.MqttSessionImpl;
import org.smartboot.mqtt.common.message.MqttPubQosMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class MqttAckProcessor<T extends MqttPubQosMessage> extends AuthorizedMqttProcessor<T> {
    @Override
    public void process0(BrokerContextImpl context, MqttSessionImpl session, T t) {
        session.getInflightQueue().notify(t);
    }
}
