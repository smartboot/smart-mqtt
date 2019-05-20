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

    void publish2Subscribers(StoredMessage pubMsg, Topic topic);


    void publish2Subscribers(StoredMessage pubMsg, Topic topic, int messageID);
}
