package org.smartboot.mqtt.broker.persistence.message;

import org.smartboot.mqtt.broker.messagebus.Message;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public interface PersistenceProvider {

    /**
     * 保存消息
     */
    void doSave(Message message);

    /**
     * 删除指定topic的所有消息
     */
    void delete(String topic);

    org.smartboot.mqtt.broker.persistence.message.Message get(String topic, long startOffset);

    long getOldestOffset(String topic);

    long getLatestOffset(String topic);

}
