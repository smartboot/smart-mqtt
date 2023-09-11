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
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/5
 */
public class KeepAliveMonitorSubscriber implements EventBusSubscriber<EventObject<MqttConnectMessage>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeepAliveMonitorSubscriber.class);
    private final BrokerContext context;

    public KeepAliveMonitorSubscriber(BrokerContext context) {
        this.context = context;
    }

    @Override
    public void subscribe(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
        //如果保持连接的值非零，并且服务端在一点五倍的保持连接时间内没有收到客户端的控制报文，
        // 它必须断开客户端的网络连接，认为网络连接已断开.
        int timeout = object.getObject().getVariableHeader().keepAliveTimeSeconds() * 1000;
        if (timeout > 0) {
            timeout += timeout >> 1;
        }
        AbstractSession session = object.getSession();
        final long finalTimeout = (timeout == 0 || timeout > context.getBrokerConfigure().getMaxKeepAliveTime()) ? context.getBrokerConfigure().getMaxKeepAliveTime() : timeout;
        context.getTimer().schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (session.isDisconnect()) {
                    LOGGER.debug("session:{} is closed, quit keepalive monitor.", session.getClientId());
                    return;
                }
                long remainingTime = finalTimeout + session.getLatestReceiveMessageTime() - System.currentTimeMillis();
                if (remainingTime > 0) {
//                    LOGGER.info("continue monitor, wait:{},current:{} latestReceiveTime:{} timeout:{}", remainingTime, System.currentTimeMillis(), session.getLatestReceiveMessageTime(), finalTimeout);
                    context.getTimer().schedule(this, remainingTime, TimeUnit.MILLISECONDS);
                } else {
                    LOGGER.debug("session:{} keepalive timeout,current:{} latestReceiveTime:{} timeout:{}", session.getClientId(), System.currentTimeMillis(), session.getLatestReceiveMessageTime(), finalTimeout);
                    session.disconnect();
                }
            }
        }, finalTimeout, TimeUnit.MILLISECONDS);
    }
}
