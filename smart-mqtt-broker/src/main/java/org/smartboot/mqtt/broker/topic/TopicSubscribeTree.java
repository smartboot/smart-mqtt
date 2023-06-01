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

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicFilterSubscriber;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/28/23
 */
public class TopicSubscribeTree {
    private final Map<MqttSession, TopicFilterSubscriber> subscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TopicSubscribeTree> subNode = new ConcurrentHashMap<>();

    public void subscribeTopic(MqttSession session, TopicFilterSubscriber subscriber) {
        TopicSubscribeTree treeNode = this;
        TopicToken token = subscriber.getTopicFilterToken();
        do {
            treeNode = treeNode.subNode.computeIfAbsent(token.getNode(), n -> new TopicSubscribeTree());
        } while ((token = token.getNextNode()) != null);
        treeNode.subscribers.put(session, subscriber);
    }

    public void unsubscribe(MqttSession session, TopicFilterSubscriber subscriber) {
        TopicSubscribeTree subscribeTree = this;
        TopicToken topicToken = subscriber.getTopicFilterToken();
        while (true) {
            subscribeTree = subscribeTree.subNode.get(topicToken.getNode());
            if (topicToken.getNextNode() == null) {
                break;
            }
            topicToken = topicToken.getNextNode();
        }
        subscribeTree.subscribers.remove(session);
    }


    public void match(TopicToken topicToken, BiConsumer<MqttSession, TopicFilterSubscriber> consumer) {
        //精确匹配
        TopicSubscribeTree subscribeTree = subNode.get(topicToken.getNode());
        if (subscribeTree != null) {
            if (topicToken.getNextNode() == null) {
                subscribers.forEach(consumer);
            } else {
                subscribeTree.match(topicToken.getNextNode(), consumer);
            }
        }
        subscribeTree = subNode.get("#");
        if (subscribeTree != null) {
            ValidateUtils.isTrue(subscribeTree.subNode.isEmpty(), "'#' node must be empty");
            subscribeTree.subscribers.forEach(consumer);
        }

        subscribeTree = subNode.get("+");
        if (subscribeTree != null) {
            if (topicToken.getNextNode() == null) {
                subscribers.forEach(consumer);
            } else {
                subscribeTree.subNode.values().forEach(t -> match(topicToken.getNextNode(), consumer));
            }
        }
    }
}
