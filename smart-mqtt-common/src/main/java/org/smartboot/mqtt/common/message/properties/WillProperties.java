package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class WillProperties {
    /**
     * 遗嘱延时间隔
     * 如果没有设置遗嘱延时间隔，遗嘱延时间隔默认值将为0，即不用延时发布遗嘱消息（Will Message）
     */
    private int willDelayInterval;

    /**
     * 载荷格式指示
     */
    private byte payloadFormatIndicator;

    /**
     * 消息过期间隔
     */
    private int messageExpiryInterval;

    /**
     * 内容类型
     */
    private String contentType;

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

    public int getWillDelayInterval() {
        return willDelayInterval;
    }

    public void setWillDelayInterval(int willDelayInterval) {
        this.willDelayInterval = willDelayInterval;
    }

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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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
}
