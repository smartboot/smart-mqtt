package org.smartboot.mqtt.broker.plugin.provider.mock;

import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.listener.TopicEventListener;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public class MockEventListenerProvider implements TopicEventListener {
    @Override
    public void onTopicCreate(BrokerTopic topic) {

    }

    @Override
    public void onPublish(MqttPublishMessage message) {

    }

}
