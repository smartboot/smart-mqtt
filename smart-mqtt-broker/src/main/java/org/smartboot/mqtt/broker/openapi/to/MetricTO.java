package org.smartboot.mqtt.broker.openapi.to;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class MetricTO {
    /**
     * 连接数
     */
    private int connectCount;

    /**
     * 主题数
     */
    private int topicCount;

    /**
     * 订阅数
     */
    private int subscriberCount;

    /**
     * 指标分组
     */
    private final Map<String, List<MetricItemTO>> group = new HashMap<>();

    public int getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(int connectCount) {
        this.connectCount = connectCount;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public Map<String, List<MetricItemTO>> getGroup() {
        return group;
    }

}
