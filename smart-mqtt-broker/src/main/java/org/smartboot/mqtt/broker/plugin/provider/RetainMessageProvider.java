package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.common.StoredMessage;

/**
 * 保留消息
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/30
 */
public interface RetainMessageProvider {

    StoredMessage get(String topic, long startOffset, long endOffset);

    void cleanTopic(String topic);

    /**
     * 存储消息
     */
    void storeRetainMessage(StoredMessage message);
}
