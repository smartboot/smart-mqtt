/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import tech.smartboot.mqtt.common.util.ValidateUtils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicToken {
    private TopicNode node;
    private final String topicFilter;
    private final TopicToken nextNode;

    public TopicToken(String node) {
        this(node, 0);
    }

    TopicToken(final String topic, final int offset) {
        int index = topic.indexOf('/', offset);
        if (index == -1) {
            this.node = updateNode(new TopicNode(offset, topic.length(), topic));
            ValidateUtils.isTrue(!this.node.contains('#') || this.node.length() == 1, "invalid topic filter");
            ValidateUtils.isTrue(!this.node.contains('+') || this.node.length() == 1, "invalid topic filter");
            this.nextNode = null;
        } else {
            this.node = updateNode(new TopicNode(offset, index, topic));
            ValidateUtils.isTrue(!this.node.contains('#'), "invalid topic filter");
            ValidateUtils.isTrue(!this.node.contains('+') || this.node.length() == 1, "invalid topic filter");
            this.nextNode = new TopicToken(topic, index + 1);
        }
        if (offset == 0) {
            this.topicFilter = topic;
        } else {
            this.topicFilter = null;
        }
    }

    private TopicNode updateNode(TopicNode node) {
        if (node.equals(TopicNode.SHARE_NODE)) {
            return TopicNode.SHARE_NODE;
        }
        if (node.equals(TopicNode.WILDCARD_PLUS_NODE)) {
            return TopicNode.WILDCARD_PLUS_NODE;
        }
        if (node.equals(TopicNode.WILDCARD_HASH_NODE)) {
            return TopicNode.WILDCARD_HASH_NODE;
        }
        return node;
    }

    public TopicNode getNode() {
        return node;
    }

    public void setNode(TopicNode node) {
        this.node = node;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public TopicToken getNextNode() {
        return nextNode;
    }

    public boolean isWildcards() {
        if (this.node == TopicNode.WILDCARD_HASH_NODE || this.node == TopicNode.WILDCARD_PLUS_NODE) {
            return true;
        }
        return nextNode != null && nextNode.isWildcards();
    }

    public boolean isShared() {
        return this.node == TopicNode.SHARE_NODE;
    }
}
