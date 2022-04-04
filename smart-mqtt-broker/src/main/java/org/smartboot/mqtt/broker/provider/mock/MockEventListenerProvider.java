package org.smartboot.mqtt.broker.provider.mock;

import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.provider.EventListenerProvider;
import org.smartboot.mqtt.common.StoredMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public class MockEventListenerProvider implements EventListenerProvider {
    @Override
    public void onTopicCreate(BrokerTopic topic) {

    }

    @Override
    public void onPublish(StoredMessage storedMessage) {

    }
}
