package org.smartboot.mqtt.broker.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentHashMap<MqttSession, MqttSession> map = new ConcurrentHashMap<>();

    public ConnectIdleTimeMonitorSubscriber(BrokerContext context) {
        this.context = context;
    }

    @Override
    public void subscribe(EventType<MqttSession> eventType, MqttSession session) {
        map.put(session, session);
        context.getEventBus().subscribe(ServerEventType.CONNECT, (eventType1, object) -> map.remove(object.getSession()));
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (map.remove(session) != null) {
                    LOGGER.info("长时间未收到Connect消息，连接断开！");
                    session.disconnect();
                }
            }
        }, context.getBrokerConfigure().getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
    }

}
