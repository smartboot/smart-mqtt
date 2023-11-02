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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/28/23
 */
public class TopicPublishTree {
    private BrokerTopic brokerTopic;
    private final ConcurrentHashMap<String, TopicPublishTree> subNode = new ConcurrentHashMap<>();

    public void addTopic(BrokerTopic brokerTopic) {
        TopicToken topicToken = brokerTopic.getTopicToken();
        TopicPublishTree treeNode = this;
        while (true) {
            treeNode = treeNode.subNode.computeIfAbsent(topicToken.getNode(), n -> new TopicPublishTree());
            if (topicToken.getNextNode() == null) {
                break;
            } else {
                topicToken = topicToken.getNextNode();
            }
        }
        treeNode.brokerTopic = brokerTopic;
    }

    public void match(TopicToken topicToken, Consumer<BrokerTopic> consumer) {
        match(this, topicToken, consumer);
    }

    private void match(TopicPublishTree treeNode, TopicToken topicToken, Consumer<BrokerTopic> consumer) {
        //匹配结束
        if (topicToken == null) {
            if (treeNode.brokerTopic != null) {
                consumer.accept(treeNode.brokerTopic);
            }
            return;
        }
        //合法的#通配符必然存在于末端
        if ("#".equals(topicToken.getNode())) {
            treeNode.subNode.values().forEach(node -> {
                subscribeChildren(node, consumer);
            });
        } else if ("+".equals(topicToken.getNode())) {
            treeNode.subNode.values().forEach(node -> {
                match(node, topicToken.getNextNode(), consumer);
            });
        } else {
            TopicPublishTree node = treeNode.subNode.get(topicToken.getNode());
            if (node != null) {
                match(node, topicToken.getNextNode(), consumer);
            }
        }
    }

    private void subscribeChildren(TopicPublishTree treeNode, Consumer<BrokerTopic> consumer) {
        BrokerTopic brokerTopic = treeNode.brokerTopic;
        if (brokerTopic != null) {
            consumer.accept(brokerTopic);
        }
        //递归订阅Topic
        treeNode.subNode.values().forEach(subNode -> subscribeChildren(subNode, consumer));
    }
}
