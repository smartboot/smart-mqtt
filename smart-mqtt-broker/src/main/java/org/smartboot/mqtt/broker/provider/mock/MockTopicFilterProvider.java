package org.smartboot.mqtt.broker.provider.mock;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.Topic;
import org.smartboot.mqtt.broker.provider.TopicFilterProvider;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class MockTopicFilterProvider implements TopicFilterProvider {
    @Override
    public void match(String topicFilter, BrokerContext context, Consumer<Topic> consumer) {
        ValidateUtils.isTrue(MqttUtil.containsTopicWildcards(topicFilter), "开源版不支持Topic通配符匹配");
        /*
         * 如果主题过滤器不同于任何现存订阅的过滤器，服务端会创建一个新的订阅并发送所有匹配的保留消息。
         */
        Topic topic = context.getOrCreateTopic(topicFilter);
        consumer.accept(topic);
    }

}
