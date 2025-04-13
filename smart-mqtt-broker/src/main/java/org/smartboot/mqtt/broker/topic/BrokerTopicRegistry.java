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

import org.smartboot.mqtt.broker.TopicSubscription;
import org.smartboot.mqtt.common.TopicToken;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MQTT主题发布树，用于管理和匹配MQTT主题的发布关系。
 * <p>
 * 该类实现了一个树形数据结构来高效地管理MQTT主题的发布关系。每个节点代表主题层级中的一个部分，
 * 支持MQTT协议中定义的三种主题匹配模式：
 * <ul>
 *   <li>精确匹配 - 如 "sensor/temperature"，完全匹配主题字符串</li>
 *   <li>单层通配符(+) - 如 "sensor/+/temperature"，匹配单个层级中的任意值</li>
 *   <li>多层通配符(#) - 如 "sensor/#"，匹配从当前层级开始的所有后续层级</li>
 * </ul>
 * </p>
 * <p>
 * 树的每个节点包含：
 * <ul>
 *   <li>当前节点的主题对象（brokerTopic）- 存储该主题的相关信息和消息队列</li>
 *   <li>子节点映射（subNode）- 存储下一层主题层级的发布树节点，使用ConcurrentHashMap保证线程安全</li>
 * </ul>
 * </p>
 * <p>
 * 该实现支持MQTT 5.0中的共享订阅功能，通过特殊的"$share"前缀来识别和处理共享订阅。
 * 共享订阅格式为：$share/{ShareName}/{TopicFilter}
 * </p>
 * <p>
 * 主题匹配算法：
 * <ol>
 *   <li>对于精确匹配，直接按层级查找对应的节点</li>
 *   <li>对于单层通配符(+)，遍历当前层级所有子节点</li>
 *   <li>对于多层通配符(#)，递归遍历所有子节点</li>
 * </ol>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/28/23
 * @see <a href="http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106">MQTT 3.1.1 主题匹配规范</a>
 * @see <a href="http://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901131">MQTT 5.0 共享订阅规范</a>
 */
public class BrokerTopicRegistry {
    /**
     * 存储当前节点的主题对象，包含主题的详细信息和消息队列。
     * <p>
     * 该字段可能为null，表示当前节点是中间节点而非叶子节点。
     * 叶子节点必须包含有效的BrokerTopic对象。
     * </p>
     */
    private BrokerTopic brokerTopic;

    /**
     * 存储子节点的发布树映射。
     * <p>
     * Key为主题层级字符串，Value为对应的发布树节点。
     * 使用ConcurrentHashMap保证在多线程环境下的线程安全性。
     * </p>
     */
    private final ConcurrentHashMap<String, BrokerTopicRegistry> subNode = new ConcurrentHashMap<>();

    /**
     * 将一个主题添加到发布树中。
     * <p>
     * 该方法会根据主题的层级结构，在发布树中创建或更新相应的节点。
     * 主题的每一层级都对应树中的一个节点，最终的叶子节点存储主题对象。
     * 实现细节：
     * <ol>
     *   <li>使用TopicToken遍历主题层级</li>
     *   <li>为每一层级创建或获取对应的BrokerTopicRegistry节点</li>
     *   <li>在最终层级设置BrokerTopic对象</li>
     * </ol>
     * </p>
     * <p>
     * 线程安全：
     * 使用ConcurrentHashMap保证线程安全，允许多线程并发添加主题。
     * </p>
     *
     * @param brokerTopic 要添加的主题对象，包含主题字符串和相关配置信息
     * @throws NullPointerException 如果brokerTopic为null
     * @see TopicToken 用于解析主题层级结构
     */
    public void registerTopic(BrokerTopic brokerTopic) {
        TopicToken topicToken = brokerTopic.getTopicToken();
        BrokerTopicRegistry treeNode = this;
        while (true) {
            treeNode = treeNode.subNode.computeIfAbsent(topicToken.getNode(), n -> new BrokerTopicRegistry());
            if (topicToken.getNextNode() == null) {
                break;
            } else {
                topicToken = topicToken.getNextNode();
            }
        }
        treeNode.brokerTopic = brokerTopic;
    }

    /**
     * 在发布树中匹配指定的主题，并对匹配的主题执行指定操作。
     * <p>
     * 该方法支持三种匹配模式：
     * <ul>
     *   <li>精确匹配 - 完全匹配主题字符串</li>
     *   <li>单层通配符(+) - 匹配任意单个层级</li>
     *   <li>多层通配符(#) - 匹配任意多个层级</li>
     * </ul>
     * 同时也支持共享订阅的匹配处理。
     * </p>
     * <p>
     * 实现细节：
     * <ol>
     *   <li>首先检查是否为共享订阅（以$share开头）</li>
     *   <li>共享订阅需要跳过前两个节点（$share和共享组名）</li>
     *   <li>调用内部match方法执行实际匹配逻辑</li>
     * </ol>
     * </p>
     * <p>
     * 线程安全：
     * 使用ConcurrentHashMap保证线程安全，允许多线程并发匹配主题。
     * </p>
     *
     * @param subscription 包含主题过滤器和订阅信息的对象
     * @param consumer 对匹配到的主题执行的操作
     * @see #match 实际执行匹配的核心方法
     */
    public void matchSubscriptionToTopics(TopicSubscription subscription, Consumer<BrokerTopic> consumer) {
        if (subscription.getTopicFilterToken().isShared()) {
            match(this, subscription.getTopicFilterToken().getNextNode().getNextNode(), consumer);
        } else {
            match(this, subscription.getTopicFilterToken(), consumer);
        }
    }

    /**
     * 在指定节点开始匹配主题，实现主题匹配的核心逻辑。
     * <p>
     * 该方法通过递归遍历实现主题的多层级匹配，支持：
     * <ul>
     *   <li>精确匹配 - 直接匹配主题层级</li>
     *   <li>单层通配符(+) - 匹配任意单个层级</li>
     *   <li>多层通配符(#) - 匹配任意多个层级</li>
     * </ul>
     * </p>
     * <p>
     * 匹配算法实现细节：
     * <ol>
     *   <li>递归终止条件：topicToken为null时检查当前节点是否有主题</li>
     *   <li>处理#通配符：必须位于末端，递归匹配所有子节点</li>
     *   <li>处理+通配符：匹配当前层级所有子节点</li>
     *   <li>精确匹配：直接查找对应子节点</li>
     * </ol>
     * </p>
     * <p>
     * 线程安全：
     * 使用ConcurrentHashMap保证线程安全，允许多线程并发匹配。
     * </p>
     *
     * @param treeNode 当前匹配的树节点
     * @param topicToken 要匹配的主题标记
     * @param consumer 对匹配到的主题执行的操作
     * @see #subscribeChildren 处理#通配符的递归匹配
     */
    private void match(BrokerTopicRegistry treeNode, TopicToken topicToken, Consumer<BrokerTopic> consumer) {
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
            BrokerTopicRegistry node = treeNode.subNode.get(topicToken.getNode());
            if (node != null) {
                match(node, topicToken.getNextNode(), consumer);
            }
        }
    }

    /**
     * 递归订阅指定节点及其所有子节点的主题。
     * <p>
     * 该方法用于处理多层通配符(#)的匹配，会遍历指定节点下的所有子节点，
     * 并对每个找到的主题执行指定的操作。
     * </p>
     *
     * @param treeNode 要遍历的树节点
     * @param consumer 对找到的主题执行的操作
     */
    private void subscribeChildren(BrokerTopicRegistry treeNode, Consumer<BrokerTopic> consumer) {
        BrokerTopic brokerTopic = treeNode.brokerTopic;
        if (brokerTopic != null) {
            consumer.accept(brokerTopic);
        }
        //递归订阅Topic
        treeNode.subNode.values().forEach(subNode -> subscribeChildren(subNode, consumer));
    }

    /**
     * 打印发布树的结构，用于调试和监控。
     * <p>
     * 以可视化的方式展示整个发布树的结构，包括：
     * <ul>
     *   <li>每个节点的主题层级</li>
     *   <li>节点之间的层级关系</li>
     *   <li>主题的完整路径</li>
     * </ul>
     * </p>
     */
    public void dump() {
        System.out.println("TopicPublishTree:");
        subNode.forEach((key, node) -> {
            System.out.println(key + (node.subNode.isEmpty() ? "" : "/-"));
            node.subNode.forEach((k, v) -> {
                System.out.print(" |: ");
                System.out.println(k + (v.subNode.isEmpty() ? "" : "/-"));
                v.dump0(1);
            });
        });
    }

    private void dump0(int depth) {
        if (brokerTopic != null) {
            System.out.println("Topic " + brokerTopic.getTopic());
            brokerTopic.dump();
        }
        subNode.forEach((key, node) -> {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.print(" +|: ");
            System.out.println(key + (node.subNode.isEmpty() ? "" : "/-"));


            node.dump0(depth + 1);
        });
    }
}
