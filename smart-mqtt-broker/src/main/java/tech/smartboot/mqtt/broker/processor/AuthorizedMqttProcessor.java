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
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public abstract class AuthorizedMqttProcessor<T extends MqttMessage> implements MqttProcessor<BrokerContextImpl, T, MqttSessionImpl> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizedMqttProcessor.class);

    @Override
    public final void process(BrokerContextImpl context, MqttSessionImpl session, T t) {
        if (session.isAuthorized()) {
            process0(context, session, t);
        } else {
            session.disconnect();
            LOGGER.error("clientId:{} is unAuthorized", session.getClientId());
        }
    }

    public abstract void process0(BrokerContextImpl context, MqttSessionImpl session, T t);
}
