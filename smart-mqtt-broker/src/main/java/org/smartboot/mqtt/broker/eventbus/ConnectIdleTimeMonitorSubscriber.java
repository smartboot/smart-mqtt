/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;

import java.util.concurrent.TimeUnit;

/**
 * 客户端与Broker建立连接后需要在特定时长内完成鉴权认证，否则需要及时断开网络连接
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class ConnectIdleTimeMonitorSubscriber implements EventBusSubscriber<MqttSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectIdleTimeMonitorSubscriber.class);
    private final BrokerContext context;

    public ConnectIdleTimeMonitorSubscriber(BrokerContext context) {
        this.context = context;
    }

    @Override
    public void subscribe(EventType<MqttSession> eventType, MqttSession session) {
        context.getTimer().newTimeout(new AsyncTask() {
            @Override
            public void execute() {
                if (!session.isAuthorized()) {
                    LOGGER.info("长时间未收到客户端：{} 的Connect消息，连接断开！", session.getClientId());
                    session.disconnect();
                }
            }
        }, context.getBrokerConfigure().getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
    }

}
