package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;

import java.util.function.Consumer;

/**
 * 主题过滤器服务提供者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public interface TopicFilterProvider {
    MqttQoS match(MqttTopicSubscription topicSubscription, BrokerContext context, Consumer<BrokerTopic> consumer);
}
