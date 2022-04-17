package org.smartboot.mqtt.broker.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.listener.MqttSessionListener;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.TimeUnit;

/**
 * 网络连接建立后，如果服务端在合理的时间内没有收到 CONNECT 报文，服务端应该关闭这个连接。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public class ConnectIdleTimeListener implements MqttSessionListener<MqttSession> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectIdleTimeListener.class);
    private final BrokerConfigure configure;

    public ConnectIdleTimeListener(BrokerConfigure configure) {
        this.configure = configure;
    }

    @Override
    public void onSessionCreate(MqttSession session) {
        LOGGER.info("注册Connect超时监听");
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (!session.isAuthorized()) {
                    LOGGER.info("长时间未收到Connect消息，连接断开！");
                    session.close();
                }
            }
        }, configure.getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
    }
}
