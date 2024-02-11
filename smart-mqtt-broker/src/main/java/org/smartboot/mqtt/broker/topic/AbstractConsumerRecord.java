/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic;

import org.smartboot.mqtt.common.TopicToken;

/**
 * Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public abstract class AbstractConsumerRecord {
    /**
     * 定义消息主题
     */
    protected final BrokerTopic topic;

    /**
     * 期望消费的点位
     */
    protected long nextConsumerOffset;

    /**
     * 最近一次订阅时间
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    private final TopicToken topicFilterToken;

    protected boolean enable = true;

    public AbstractConsumerRecord(BrokerTopic topic, TopicToken topicFilterToken, long nextConsumerOffset) {
        this.topic = topic;
        this.topicFilterToken = topicFilterToken;
        this.nextConsumerOffset = nextConsumerOffset;
    }

    /**
     * 推送消息到客户端
     */
    public abstract void pushToClient();

    public final BrokerTopic getTopic() {
        return topic;
    }

    public final long getLatestSubscribeTime() {
        return latestSubscribeTime;
    }

    public final TopicToken getTopicFilterToken() {
        return topicFilterToken;
    }

    public final long getNextConsumerOffset() {
        return nextConsumerOffset;
    }

    public final void disable() {
        this.enable = false;
    }

}
