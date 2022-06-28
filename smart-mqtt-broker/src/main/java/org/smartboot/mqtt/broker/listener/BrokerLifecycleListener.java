package org.smartboot.mqtt.broker.listener;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;

import java.util.EventListener;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/5
 */
public interface BrokerLifecycleListener extends EventListener {
    void onStarted(BrokerContext context);

    void onDestroy(BrokerContext context);

    /**
     * 客户端建立连接
     *
     * @param session
     */
    void onConnect(MqttSession session);

    /**
     * 客户端断开连接
     *
     * @param session
     */
    void onDisconnect(MqttSession session);

    /**
     * 订阅Topic
     *
     * @param session
     */
    void onSubscribe(TopicSubscriber session);
}
