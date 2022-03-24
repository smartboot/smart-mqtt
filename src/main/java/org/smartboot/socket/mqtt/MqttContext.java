package org.smartboot.socket.mqtt;

import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public interface MqttContext {

    public MqttSession addSession(MqttSession session);

    boolean removeSession(MqttSession session);

    void removeSession(String clientId);

    MqttSession getSession(String clientId);

    /**
     * 取消订阅
     */
    void unSubscribe(String clientId, String topicFilter);

    /**
     * 获取Topic，如果不存在将创建
     *
     * @param topic
     * @return
     */
    Topic getOrCreateTopic(String topic);

    /**
     * 发送消息至所有客户端
     *
     * @param pubMsg
     * @param topic
     */
    void publish2Subscribers(StoredMessage pubMsg, Topic topic);


    void publish2Subscribers(StoredMessage pubMsg, Topic topic, int messageID);
}
