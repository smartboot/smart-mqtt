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

public class Topic extends ToString {
    /**
     * 主题名
     */
    private final String topic;

    private final TopicToken topicToken;

    public Topic(String topic) {
        this.topic = topic;
        this.topicToken = new TopicToken(topic);
    }

    public TopicToken getTopicToken() {
        return topicToken;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public String toString() {
        return topic;
    }
}
