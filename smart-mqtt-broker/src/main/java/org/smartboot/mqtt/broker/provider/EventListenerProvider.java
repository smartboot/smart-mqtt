package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.Topic;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public interface EventListenerProvider {
    void onTopicCreate(Topic topic);
}
