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
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * topic订阅关系树
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/28/23
 */
public class TopicSubscribeTree {
    /**
     * 当前节点的订阅关系
     */
    private final Map<MqttSession, TopicSubscriber> subscribers = new ConcurrentHashMap<>();
    /**
     * 子节点订阅关系
     */
    private final ConcurrentHashMap<String, TopicSubscribeTree> subNode = new ConcurrentHashMap<>();

    /**
     * 将此订阅注册到关系树
     */
    public void subscribeTopic(MqttSession session, TopicSubscriber subscriber) {
        TopicSubscribeTree treeNode = this;
        TopicToken token = subscriber.getTopicFilterToken();
        do {
            treeNode = treeNode.subNode.computeIfAbsent(token.getNode(), n -> new TopicSubscribeTree());
        } while ((token = token.getNextNode()) != null);
        treeNode.subscribers.put(session, subscriber);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(MqttSession session, TopicSubscriber subscriber) {
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

    /**
     * 新增的Topic触发与订阅树匹配关系的刷新
     */
    public void refreshWhenTopicCreated(BrokerTopic topicToken) {
        BiConsumer<MqttSession, TopicSubscriber> consumer = (session, topicSubscriber) -> {
            session.subscribeSuccess(topicSubscriber, topicToken);
        };
        //遍历共享订阅
        TopicSubscribeTree shareTree = subNode.get("$share");
        if (shareTree != null) {
            shareTree.subNode.values().forEach(tree -> tree.match0(topicToken.getTopicToken(), consumer));
        }
        //遍历普通订阅
        match0(topicToken.getTopicToken(), consumer);
    }

    private void match0(TopicToken topicToken, BiConsumer<MqttSession, TopicSubscriber> consumer) {
        //精确匹配
        TopicSubscribeTree subscribeTree = subNode.get(topicToken.getNode());
        if (subscribeTree != null) {
            if (topicToken.getNextNode() == null) {
                subscribeTree.subscribers.forEach(consumer);
            } else {
                subscribeTree.match0(topicToken.getNextNode(), consumer);
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
                subscribeTree.subNode.values().forEach(t -> match0(topicToken.getNextNode(), consumer));
            }
        }
    }

    public void dump() {
        System.out.println("订阅拓扑:");
        dump0(0);
    }

    private void dump0(int level) {
        if (!subscribers.isEmpty()) {
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println("|- clients:(" + subscribers.size() + ")");
        }

        subscribers.keySet().forEach(session -> {
            System.out.print("  ");
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println("|- " + session.getClientId());
        });
        subNode.forEach((node, tree) -> {
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println(node + (tree.subNode.isEmpty() && tree.subscribers.isEmpty() ? "" : "/"));
            tree.dump0(level + 1);
        });
    }
}
