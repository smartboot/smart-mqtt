package org.smartboot.socket.mqtt;

import org.smartboot.socket.mqtt.common.Topic;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.push.PushListener;
import org.smartboot.socket.mqtt.store.StoredMessage;

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
