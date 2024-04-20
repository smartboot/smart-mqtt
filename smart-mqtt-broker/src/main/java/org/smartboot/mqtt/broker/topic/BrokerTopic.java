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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.TopicToken;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * Broker端的Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class BrokerTopic {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerTopic.class);

    /**
     * 默认订阅组
     */
    private final SubscriberGroup defaultGroup = new SubscriberGroup();
    /**
     * 订阅组
     */
    private final Map<String, SubscriberGroup> shareSubscribers = new ConcurrentHashMap<>();
    /**
     * 当前Topic是否圈闭推送完成
     */
    private final Semaphore semaphore = new Semaphore(1);
    private final TopicToken topicToken;
    private final ExecutorService executorService;

    private boolean enabled = true;

    private volatile int version = 0;

    private final AsyncTask asyncTask = new AsyncTask() {
        @Override
        public void execute() {
            AbstractConsumerRecord subscriber;
            queue.offer(BREAK);
            int mark = version;
            while ((subscriber = queue.poll()) != BREAK) {
                try {
                    subscriber.pushToClient();
                } catch (Exception e) {
                    LOGGER.error("batch publish exception:{}", e.getMessage(), e);
                }
            }
            semaphore.release();
            if (mark != version && !queue.isEmpty()) {
                push();
            }
        }
    };

    /**
     * 保留消息
     */
    private Message retainMessage;

    // 消息队列
    private final MessageQueue messageQueue = new MemoryMessageStoreQueue();
    /**
     * 当前Topic处于监听状态的订阅者
     */
    private final ConcurrentLinkedQueue<AbstractConsumerRecord> queue = new ConcurrentLinkedQueue<>();

    private static final AbstractConsumerRecord BREAK = new AbstractConsumerRecord(null, null, -1) {
        @Override
        public void pushToClient() {
            throw new UnsupportedOperationException();
        }
    };

    public BrokerTopic(String topic) {
        this(topic, null);
    }

    public BrokerTopic(String topic, ExecutorService executorService) {
        this.topicToken = new TopicToken(topic);
        this.executorService = executorService;
    }


    public SubscriberGroup getSubscriberGroup(TopicToken topicToken) {
        if (topicToken.isShared()) {
            return shareSubscribers.computeIfAbsent(topicToken.getTopicFilter(), s -> new SubscriberSharedGroup(topicToken, BrokerTopic.this));
        } else {
            return defaultGroup;
        }
    }

    void removeShareGroup(String topicFilter) {
        shareSubscribers.remove(topicFilter);
    }

    /**
     * 是否存在订阅者
     * @return
     */
    public boolean isNoneSubscriber() {
        return shareSubscribers.isEmpty() && defaultGroup.isEmpty();
    }

    public void addSubscriber(AbstractConsumerRecord subscriber) {
        queue.offer(subscriber);
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

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    @Override
    public String toString() {
        return getTopic();
    }

    /**
     * 触发消息推送
     */
    public void push() {
        if (enabled && semaphore.tryAcquire()) {
            //已加入推送队列
            executorService.execute(asyncTask);
        }
    }

    public void addVersion() {
        version++;
    }

    public void disable() {
        this.enabled = false;
    }
}
