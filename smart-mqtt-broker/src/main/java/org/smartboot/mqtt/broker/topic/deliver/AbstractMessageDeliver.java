/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic.deliver;


import org.smartboot.mqtt.broker.TopicSubscription;
import org.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.plugin.spec.MessageDeliver;

/**
 * MQTT消息消费的抽象基类，负责管理消息的消费状态和推送机制。
 * <p>
 * 该类维护了消息消费的核心信息，包括：
 * <ul>
 *   <li>消息主题（Topic）及其订阅关系</li>
 *   <li>消息消费的位置信息（Offset）</li>
 *   <li>订阅时间和消费状态</li>
 * </ul>
 * </p>
 * <p>
 * 消息推送机制：
 * <ul>
 *   <li>支持异步消息推送，由具体实现类定义推送策略</li>
 *   <li>通过enable标志控制消费者的活跃状态</li>
 *   <li>维护消费位置，确保消息按序消费</li>
 * </ul>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public abstract class AbstractMessageDeliver implements MessageDeliver {
    /**
     * 消息主题对象，维护主题的订阅关系和消息存储。
     * <p>
     * 包含主题的完整信息，如主题名称、订阅者列表、消息队列等，
     * 用于消息的存储和分发。
     * </p>
     */
    protected final BrokerTopicImpl topic;

    /**
     * 下一条待消费消息的位置标识。
     * <p>
     * 用于追踪消息消费进度，确保消息按序消费，避免重复消费或遗漏。
     * 当消息成功消费后，此值会相应递增。
     * </p>
     */
    protected long nextConsumerOffset;

    /**
     * 最近一次订阅的时间戳。
     * <p>
     * 记录订阅创建或更新的时间，用于：
     * <ul>
     *   <li>判断订阅的活跃度</li>
     *   <li>处理订阅过期逻辑</li>
     *   <li>监控订阅状态变化</li>
     * </ul>
     * </p>
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    /**
     * 主题订阅关系对象，包含订阅的详细配置。
     * <p>
     * 存储了订阅的QoS级别、主题过滤器等信息，
     * 用于消息投递时的服务质量控制。
     * </p>
     */
    private final TopicSubscription topicFilterToken;

    /**
     * 消费者状态标志。
     * <p>
     * true表示消费者处于活跃状态，可以接收和处理消息；
     * false表示消费者已禁用，暂停接收新消息。
     * </p>
     */
    protected boolean enable = true;

    public AbstractMessageDeliver(BrokerTopicImpl topic, TopicSubscription topicFilterToken, long nextConsumerOffset) {
        this.topic = topic;
        this.topicFilterToken = topicFilterToken;
        this.nextConsumerOffset = nextConsumerOffset;
    }

    /**
     * 将消息推送到客户端的抽象方法。
     * <p>
     * 具体的消息推送逻辑由子类实现，可能包括：
     * <ul>
     *   <li>消息的QoS处理</li>
     *   <li>消息的重传机制</li>
     *   <li>消息的确认机制</li>
     *   <li>消息的过滤规则</li>
     * </ul>
     * </p>
     */
    public abstract void pushToClient();

    public final BrokerTopicImpl getTopic() {
        return topic;
    }

    public final long getLatestSubscribeTime() {
        return latestSubscribeTime;
    }

    public final TopicToken getTopicFilterToken() {
        return topicFilterToken.getTopicFilterToken();
    }

    @Override
    public MqttQoS getMqttQoS() {
        return topicFilterToken.getMqttQoS();
    }

    public final long getNextConsumerOffset() {
        return nextConsumerOffset;
    }

    public final void disable() {
        this.enable = false;
    }

    public boolean isEnable() {
        return enable;
    }
}
