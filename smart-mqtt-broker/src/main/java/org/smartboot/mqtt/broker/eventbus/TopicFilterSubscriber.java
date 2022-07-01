package org.smartboot.mqtt.broker.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.plugin.provider.TopicFilterProvider;
import org.smartboot.mqtt.broker.plugin.provider.TopicTokenUtil;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/30
 */
public class TopicFilterSubscriber implements EventBusSubscriber<BrokerTopic>, TopicFilterProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicFilterSubscriber.class);
    /**
     * 通配符注册的监听事件
     */
    private final ConcurrentMap<TopicToken, ConcurrentLinkedQueue<Consumer<BrokerTopic>>> wildcardsQueue = new ConcurrentHashMap<>();

    @Override
    public MqttQoS match(MqttTopicSubscription topicSubscription, BrokerContext context, Consumer<BrokerTopic> consumer) {
        String topicFilter = topicSubscription.getTopicFilter();
        TopicToken topicToken = new TopicToken(topicFilter);
        //精准匹配
        if (!topicToken.isWildcards()) {
            BrokerTopic topic = context.getOrCreateTopic(topicFilter);
            consumer.accept(topic);
            return topicSubscription.getQualityOfService();
        }

        //通配符匹配
        for (BrokerTopic topic : context.getTopics()) {
            if (TopicTokenUtil.match(topic.getTopicToken(), topicToken)) {
                consumer.accept(topic);
            }
        }
        ConcurrentLinkedQueue<Consumer<BrokerTopic>> linkedQueue = wildcardsQueue.computeIfAbsent(topicToken, s -> new ConcurrentLinkedQueue<>());
        linkedQueue.offer(consumer);
        return topicSubscription.getQualityOfService();
    }

    @Override
    public void subscribe(EventType<BrokerTopic> eventType, BrokerTopic pubTopic) {
        wildcardsQueue.forEach((topicToken, queue) -> {
            if (TopicTokenUtil.match(pubTopic.getTopicToken(), topicToken)) {
                for (Consumer<BrokerTopic> consumer : queue) {
                    consumer.accept(pubTopic);
                }
            }
        });
    }
}
