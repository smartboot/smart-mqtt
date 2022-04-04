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
