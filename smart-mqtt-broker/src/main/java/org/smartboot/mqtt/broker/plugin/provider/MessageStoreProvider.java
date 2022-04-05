package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.store.MessageQueue;
import org.smartboot.mqtt.common.StoredMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public interface MessageStoreProvider {
    MessageQueue getStoreQueue(String topic);

    void cleanTopic(String topic);

    /**
     * 存储消息
     */
    void storeTopic(StoredMessage storedMessage);
}
