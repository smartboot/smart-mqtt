package org.smartboot.mqtt.client;

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
public class ClientQosPublisher extends QosPublisher {

    @Override
    protected void retry(CompletableFuture<Boolean> future, AbstractSession session, MqttMessage mqttMessage) {
        //注册重试
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (!future.isDone()) {
                    session.write(mqttMessage);
                }
            }
        }, 1, TimeUnit.SECONDS);
    }
}
