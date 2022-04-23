package org.smartboot.mqtt.broker.plugin.provider.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.plugin.provider.TopicFilterProvider;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class MockTopicFilterProvider implements TopicFilterProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockTopicFilterProvider.class);

    @Override
    public MqttQoS match(MqttTopicSubscription topicSubscription, BrokerContext context, Consumer<BrokerTopic> consumer) {
        String topicFilter = topicSubscription.getTopicFilter();
        if (MqttUtil.containsTopicWildcards(topicFilter)) {
            LOGGER.error("开源版不支持Topic通配符匹配，若有需求请升级为企业版!");
            return MqttQoS.FAILURE;
        }
        /*
         * 如果主题过滤器不同于任何现存订阅的过滤器，服务端会创建一个新的订阅并发送所有匹配的保留消息。
         */
        BrokerTopic topic = context.getOrCreateTopic(topicFilter);
        consumer.accept(topic);
        return topicSubscription.getQualityOfService();
    }

}
