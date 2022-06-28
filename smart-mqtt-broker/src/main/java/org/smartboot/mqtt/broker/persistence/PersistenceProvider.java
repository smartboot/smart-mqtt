package org.smartboot.mqtt.broker.persistence;

import org.smartboot.mqtt.broker.eventbus.EventMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public interface PersistenceProvider {

    /**
     * 保存消息
     */
    void doSave(EventMessage message);

    void delete(String topic);

    Message get(String topic, long startOffset);

    long getOldestOffset(String topic);

    long getLatestOffset(String topic);

}
