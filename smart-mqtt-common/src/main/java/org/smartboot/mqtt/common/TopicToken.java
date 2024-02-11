/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.util.ValidateUtils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicToken {
    private final String node;
    private final String topicFilter;
    private final TopicToken nextNode;

    public TopicToken(String node) {
        this(node, 0);
    }

    TopicToken(String node, int offset) {
        int index = node.indexOf('/', offset);
        if (index == -1) {
            this.node = node.substring(offset);
            ValidateUtils.isTrue(this.node.indexOf('#') == -1 || this.node.length() == 1, "invalid topic filter");
            ValidateUtils.isTrue(this.node.indexOf('+') == -1 || this.node.length() == 1, "invalid topic filter");
            this.nextNode = null;
        } else {
            this.node = node.substring(offset, index);
            ValidateUtils.isTrue(this.node.indexOf('#') == -1, "invalid topic filter");
            ValidateUtils.isTrue(this.node.indexOf('+') == -1 || this.node.length() == 1, "invalid topic filter");
            this.nextNode = new TopicToken(node, index + 1);
        }
        if (offset == 0) {
            this.topicFilter = node;
        } else {
            this.topicFilter = null;
        }
    }

    public String getNode() {
        return node;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public TopicToken getNextNode() {
        return nextNode;
    }

    public boolean isWildcards() {
        if (this.node.equals("+") || this.node.equals("#")) {
            return true;
        }
        return nextNode != null && nextNode.isWildcards();
    }

    public boolean isShared() {
        return this.node.equals("$share");
    }
}
