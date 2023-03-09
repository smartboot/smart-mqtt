package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/25
 */
public class BrokerQosPublisher extends QosPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerQosPublisher.class);
    private final BrokerContext mqttContext;

    public BrokerQosPublisher(BrokerContext mqttContext) {
        this.mqttContext = mqttContext;
    }

    @Override
    protected void retry(CompletableFuture<Boolean> future, AbstractSession session, MqttMessage mqttMessage) {
        //注册重试
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (!future.isDone()) {
                    // 如果客户端发生过断链,则 mqttSession!=session
                    LOGGER.info("retry...");
                    MqttSession mqttSession = mqttContext.getSession(session.getClientId());
                    mqttSession.write(mqttMessage);
                }
            }
        }, 1, TimeUnit.SECONDS);
    }
}
