/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.common.TopicToken;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Broker端的Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class BrokerTopic {
    /**
     * 当前订阅的消费者
     */
    private final Map<MqttSession, TopicSubscriber> consumeOffsets = new ConcurrentHashMap<>();
    private final AtomicInteger version = new AtomicInteger();
    /**
     * 当前Topic是否圈闭推送完成
     */
    private final Semaphore semaphore = new Semaphore(1);
    private final TopicToken topicToken;

    /**
     * 保留消息
     */
    private Message retainMessage;

    /**
     * 当前Topic处于监听状态的订阅者
     */
    private final ConcurrentLinkedQueue<TopicSubscriber> queue = new ConcurrentLinkedQueue<>();

    public BrokerTopic(String topic) {
        this.topicToken = new TopicToken(topic);
    }

    public Map<MqttSession, TopicSubscriber> getConsumeOffsets() {
        return consumeOffsets;
    }

    public AtomicInteger getVersion() {
        return version;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public ConcurrentLinkedQueue<TopicSubscriber> getQueue() {
        return queue;
    }

    public TopicToken getTopicToken() {
        return topicToken;
    }

    public String getTopic() {
        return topicToken.getTopicFilter();
    }

    public Message getRetainMessage() {
        return retainMessage;
    }

    public void setRetainMessage(Message retainMessage) {
        this.retainMessage = retainMessage;
    }

    @Override
    public String toString() {
        return getTopic();
    }
}
