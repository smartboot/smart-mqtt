package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.store.MessageQueue;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * 消息存储服务
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public interface MessageStoreProvider {
    MessageQueue getStoreQueue(String topic);

    void cleanTopic(String topic);


    /**
     * 存储消息
     */
    StoredMessage storeMessage(String clientId, MqttPublishMessage storedMessage);
}
