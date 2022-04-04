package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.common.StoredMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public interface EventListenerProvider {
    void onTopicCreate(BrokerTopic topic);

    void onPublish(StoredMessage storedMessage);
}
