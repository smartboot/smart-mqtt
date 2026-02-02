/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.bus.event;

import org.smartboot.socket.timer.TimerTask;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.bus.AsyncEventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/5
 */
public class KeepAliveMonitorSubscriber implements EventBusConsumer<AsyncEventObject<MqttConnectMessage>> {
    private final BrokerContext context;

    public KeepAliveMonitorSubscriber(BrokerContext context) {
        this.context = context;
    }

    @Override
    public void consumer(EventType<AsyncEventObject<MqttConnectMessage>> eventType, AsyncEventObject<MqttConnectMessage> object) {
        //如果保持连接的值非零，并且服务端在一点五倍的保持连接时间内没有收到客户端的控制报文，
        // 它必须断开客户端的网络连接，认为网络连接已断开.
        int timeout = object.getObject().getVariableHeader().keepAliveTimeSeconds() * 1000;
        if (timeout > 0) {
            timeout += timeout >> 1;
        }
        MqttSessionImpl session = (MqttSessionImpl) object.getSession();
        final long finalTimeout = (timeout == 0 || timeout > context.Options().getMaxKeepAliveTime()) ? context.Options().getMaxKeepAliveTime() : timeout;
        TimerTask task = context.getTimer().schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (session.isDisconnect()) {
//                    LOGGER.info("session:{} is closed, quit keepalive monitor.", session.getClientId());
                    return;
                }
                long remainingTime = finalTimeout + session.getLatestReceiveMessageTime() - System.currentTimeMillis();
                if (remainingTime > 0) {
//                    LOGGER.info("continue monitor, wait:{},current:{} latestReceiveTime:{} timeout:{}", remainingTime, System.currentTimeMillis(), session.getLatestReceiveMessageTime(), finalTimeout);
                    session.setKeepAliveTimer(context.getTimer().schedule(this, remainingTime, TimeUnit.MILLISECONDS));
                } else {
//                    LOGGER.info("session:{} keepalive timeout,current:{} latestReceiveTime:{} timeout:{}", session.getClientId(), System.currentTimeMillis(), session.getLatestReceiveMessageTime(), finalTimeout);
                    session.disconnect();
                }
            }
        }, finalTimeout, TimeUnit.MILLISECONDS);
        session.setKeepAliveTimer(task);
        object.getFuture().complete(null);
    }
}
