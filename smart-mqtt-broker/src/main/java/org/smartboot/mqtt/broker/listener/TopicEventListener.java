package org.smartboot.mqtt.broker.listener;

import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.common.StoredMessage;

import java.util.EventListener;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public interface TopicEventListener extends EventListener {
    void onTopicCreate(BrokerTopic topic);

    void onPublish(StoredMessage storedMessage);
}
