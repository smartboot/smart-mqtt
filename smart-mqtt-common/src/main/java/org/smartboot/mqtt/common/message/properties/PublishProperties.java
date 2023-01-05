package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/5
 */
public class PublishProperties {
    /**
     * 载荷格式指示
     */
    private byte payloadFormatIndicator;

    /**
     * 消息过期间隔
     */
    private int messageExpiryInterval;
    /**
     * 主题别名
     */
    private int topicAlias;

    /**
     * 响应主题
     */
    private String responseTopic;

    /**
     * 对比数据
     */
    private byte[] correlationData;

    /**
     * 用户属性
     */
    private final List<UserProperty> userProperties = new ArrayList<>();
    /**
     * 订阅标识符
     */
    private int subscriptionIdentifier;

    /**
     * 内容类型
     */
    private String contentType;

    public byte getPayloadFormatIndicator() {
        return payloadFormatIndicator;
    }

    public void setPayloadFormatIndicator(byte payloadFormatIndicator) {
        this.payloadFormatIndicator = payloadFormatIndicator;
    }

    public int getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public void setMessageExpiryInterval(int messageExpiryInterval) {
        this.messageExpiryInterval = messageExpiryInterval;
    }

    public int getTopicAlias() {
        return topicAlias;
    }

    public void setTopicAlias(int topicAlias) {
        this.topicAlias = topicAlias;
    }

    public String getResponseTopic() {
        return responseTopic;
    }

    public void setResponseTopic(String responseTopic) {
        this.responseTopic = responseTopic;
    }

    public byte[] getCorrelationData() {
        return correlationData;
    }

    public void setCorrelationData(byte[] correlationData) {
        this.correlationData = correlationData;
    }

    public List<UserProperty> getUserProperties() {
        return userProperties;
    }

    public int getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    public void setSubscriptionIdentifier(int subscriptionIdentifier) {
        this.subscriptionIdentifier = subscriptionIdentifier;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

