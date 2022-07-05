package org.smartboot.mqtt.broker.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.TimeUnit;

/**
 * 客户端与Broker建立连接后需要在特定时长内完成鉴权认证，否则需要及时断开网络连接
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class ConnectIdleTimeMonitorSubscriber implements EventBusSubscriber<MqttSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectIdleTimeMonitorSubscriber.class);
    private final BrokerConfigure configure;

    public ConnectIdleTimeMonitorSubscriber(BrokerConfigure configure) {
        this.configure = configure;
    }

    @Override
    public void subscribe(EventType<MqttSession> eventType, MqttSession session) {
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (!session.isAuthorized()) {
                    LOGGER.info("长时间未收到Connect消息，连接断开！");
                    session.disconnect();
                }
            }
        }, configure.getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
    }
}