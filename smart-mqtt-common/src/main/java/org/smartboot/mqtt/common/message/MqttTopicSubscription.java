/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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