/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.common.TopicNode;
import tech.smartboot.mqtt.common.TopicToken;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MQTT主题订阅树，用于管理客户端的主题订阅关系。
 * <p>
 * 该类实现了一个树形数据结构来高效地管理MQTT主题的订阅关系。每个节点代表主题层级中的一个部分，
 * 支持MQTT协议中定义的三种订阅模式：
 * <ul>
 *   <li>精确匹配 - 如 "sensor/temperature"</li>
 *   <li>单层通配符(+) - 如 "sensor/+/temperature"，匹配单个层级</li>
 *   <li>多层通配符(#) - 如 "sensor/#"，匹配多个层级</li>
 * </ul>
 * </p>
 * <p>
 * 树的每个节点包含：
 * <ul>
 *   <li>当前节点的订阅者集合（subscribers）- 存储订阅到当前主题层级的会话</li>
 *   <li>子节点映射（subNode）- 存储下一层主题层级的订阅树节点</li>
 * </ul>
 * </p>
 * <p>
 * 该实现还支持MQTT 5.0中的共享订阅功能，通过特殊的"$share"前缀来识别和处理共享订阅。
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/28/23
 */
class SubscribeRelationMatcher {
    private static final Set<SessionSubscribeRelation> EMPTY_SUBSCRIBERS = Collections.emptySet();
    private static final Map<TopicNode, SubscribeRelationMatcher> EMPTY_MAP = Collections.emptyMap();
    /**
     * 存储当前节点的订阅关系映射。
     * <p>
     * Key为MQTT客户端会话（MqttSession），Value为该会话的主题订阅信息（TopicSubscriber）。
     * 使用ConcurrentHashMap保证在多线程环境下的线程安全性。
     * </p>
     */
    private Set<SessionSubscribeRelation> subscribers = EMPTY_SUBSCRIBERS;

    /**
     * 存储子节点的订阅树映射。
     * <p>
     * Key为主题层级字符串，Value为对应的订阅树节点。
     * 例如，对于主题"sensor/temperature"，第一层"sensor"和第二层"temperature"分别对应一个子节点。
     * 特殊节点：
     * <ul>
     *   <li>'+' - 单层通配符节点</li>
     *   <li>'#' - 多层通配符节点</li>
     *   <li>'$share' - 共享订阅节点</li>
     * </ul>
     * </p>
     */
    private volatile Map<TopicNode, SubscribeRelationMatcher> subNode = EMPTY_MAP;


    /**
     * 将客户端的主题订阅注册到订阅树中。
     * <p>
     * 该方法会根据主题过滤器（Topic Filter）的层级结构，在订阅树中创建或更新相应的节点，
     * 并在最终的叶子节点上保存会话的订阅关系。支持通配符（+和#）和共享订阅。
     * </p>
     *
     * @param subscriber 包含主题过滤器和QoS等订阅信息的对象
     */
    public void add(SessionSubscribeRelation subscriber) {
        SubscribeRelationMatcher treeNode = this;
        TopicToken token = subscriber.getTopicFilterToken();
        do {
            if (treeNode.subNode == EMPTY_MAP) {
                synchronized (treeNode) {
                    if (treeNode.subNode == EMPTY_MAP) {
                        treeNode.subNode = new ConcurrentHashMap<>();
                    }
                }
            }
            treeNode = treeNode.subNode.computeIfAbsent(token.getNode(), n -> new SubscribeRelationMatcher());
        } while ((token = token.getNextNode()) != null);
        treeNode.add0(subscriber);
    }

    private synchronized void add0(SessionSubscribeRelation subscriber) {
        if (subscribers == EMPTY_SUBSCRIBERS) {
            subscribers = ConcurrentHashMap.newKeySet();
        }
        subscribers.add(subscriber);
    }

    /**
     * 取消客户端对指定主题的订阅。
     * <p>
     * 该方法会遍历订阅树，找到对应的主题节点，并移除该会话的订阅关系。
     * 注意：该方法只移除订阅关系，不会删除空的节点。
     * </p>
     *
     * @param subscriber 包含要取消订阅的主题过滤器信息的对象
     */
    public void remove(SessionSubscribeRelation subscriber) {
        SubscribeRelationMatcher subscribeTree = this;
        TopicToken topicToken = subscriber.getTopicFilterToken();
        while (true) {
            subscribeTree = subscribeTree.subNode.get(topicToken.getNode());
            if (topicToken.getNextNode() == null) {
                break;
            }
            topicToken = topicToken.getNextNode();
        }
        subscribeTree.remove0(subscriber);
    }

    private synchronized void remove0(SessionSubscribeRelation subscriber) {
        subscribers.remove(subscriber);
        if (subscribers.isEmpty()) {
            subscribers = EMPTY_SUBSCRIBERS;
        }
    }

    /**
     * 当新的主题被创建时，刷新该主题与现有订阅关系的匹配。
     * <p>
     * 该方法会遍历订阅树，找到所有匹配新主题的订阅关系（包括精确匹配、通配符匹配和共享订阅），
     * 并触发相应的订阅成功回调。这个过程确保了新主题的消息能够正确地推送给所有匹配的订阅者。
     * </p>
     *
     * @param brokerTopic 新创建的主题对象
     */
    public void match(BrokerTopicImpl brokerTopic) {
        Consumer<SessionSubscribeRelation> consumer = (topicSubscription) -> topicSubscription.getMqttSession().subscribeSuccess(topicSubscription, brokerTopic);
        //遍历共享订阅
        SubscribeRelationMatcher shareTree = subNode.get(TopicNode.SHARE_NODE);
        if (shareTree != null) {
            shareTree.subNode.values().forEach(tree -> tree.match0(brokerTopic, consumer));
        }
        //遍历普通订阅
        match0(brokerTopic, consumer);
    }

    /**
     * 在订阅树中匹配指定主题，并对匹配的订阅者执行指定操作。
     * <p>
     * 该方法实现了MQTT主题匹配的核心逻辑，包括：
     * <ul>
     *   <li>精确匹配 - 直接匹配主题层级</li>
     *   <li>单层通配符(+)匹配 - 匹配任意单个层级</li>
     *   <li>多层通配符(#)匹配 - 匹配任意多个层级</li>
     * </ul>
     * </p>
     *
     * @param topicToken 要匹配的主题标记
     * @param consumer   对匹配的订阅者执行的操作
     */
    private void match0(TopicToken topicToken, Consumer<SessionSubscribeRelation> consumer) {
        //精确匹配
        SubscribeRelationMatcher subscribeTree = subNode.get(topicToken.getNode());
        if (subscribeTree != null) {
            if (topicToken.getNextNode() == null) {
                subscribeTree.subscribers.forEach(consumer);
            } else {
                subscribeTree.match0(topicToken.getNextNode(), consumer);
            }
        }
        subscribeTree = subNode.get(TopicNode.WILDCARD_HASH_NODE);
        if (subscribeTree != null) {
            ValidateUtils.isTrue(subscribeTree.subNode.isEmpty(), "'#' node must be empty");
            subscribeTree.subscribers.forEach(consumer);
        }

        subscribeTree = subNode.get(TopicNode.WILDCARD_PLUS_NODE);
        if (subscribeTree != null) {
            if (topicToken.getNextNode() == null) {
                subscribers.forEach(consumer);
            } else {
                subscribeTree.match0(topicToken.getNextNode(), consumer);
            }
        }
    }

    /**
     * 打印订阅树的结构，用于调试和监控。
     * <p>
     * 以可视化的方式展示整个订阅树的结构，包括：
     * <ul>
     *   <li>每个节点的主题层级</li>
     *   <li>订阅者数量</li>
     *   <li>订阅者的客户端标识</li>
     * </ul>
     * </p>
     */
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

        subscribers.forEach(relation -> {
            System.out.print("  ");
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println("|- " + relation.getMqttSession().getClientId());
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
