package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttQoS;

public final class MqttTopicSubscription extends ToString {
    /**
     * 主题过滤器
     */
    private String topicFilter;
    private MqttQoS qualityOfService;

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    public void setQualityOfService(MqttQoS qualityOfService) {
        this.qualityOfService = qualityOfService;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public MqttQoS getQualityOfService() {
        return qualityOfService;
    }
}