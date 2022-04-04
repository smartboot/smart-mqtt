package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.util.ValidateUtils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicToken {
    private final String node;

    private TopicToken nextNode;

    public TopicToken(String node) {
        this(node, 0);
    }

    public TopicToken(String node, int offset) {
        int index = node.indexOf('/', offset);
        if (index == -1) {
            this.node = node.substring(offset);
            ValidateUtils.isTrue(this.node.indexOf('#') == -1 || this.node.length() == 1, "invalid topic filter");
            ValidateUtils.isTrue(this.node.indexOf('+') == -1 || this.node.length() == 1, "invalid topic filter");
        } else {
            this.node = node.substring(offset, index);
            ValidateUtils.isTrue(this.node.indexOf('#') == -1, "invalid topic filter");
            ValidateUtils.isTrue(this.node.indexOf('+') == -1 || this.node.length() == 1, "invalid topic filter");
            this.nextNode = new TopicToken(node, index + 1);
        }
    }

    public String getNode() {
        return node;
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
}
