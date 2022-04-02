package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.Topic;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public interface TopicFilterProvider {
    void match(String topic, BrokerContext context, Consumer<Topic> consumer);

    void rematch(Topic newTopic);
}
