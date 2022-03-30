package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.push.PushListener;
import org.smartboot.mqtt.broker.store.StoredMessage;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public interface MqttContext {

    MqttSession addSession(MqttSession session);

    boolean removeSession(MqttSession session);

    MqttSession getSession(String clientId);

    /**
     * 获取Topic，如果不存在将创建
     *
     * @param topic
     * @return
     */
    Topic getOrCreateTopic(String topic);

    PushListener getTopicListener();

    /**
     * 发送消息至订阅者
     */
    void publish(Topic topic, StoredMessage storedMessage);

}
