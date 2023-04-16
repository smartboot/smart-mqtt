/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端订阅的Topic，可能是通配符订阅
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/13
 */
class TopicFilterSubscriber {
    private final TopicToken topicFilterToken;

    private final MqttQoS mqttQoS;

    /**
     * 客户端订阅所匹配的Topic。通配符订阅时可能有多个
     */
    private final Map<String, TopicSubscriber> topicSubscribers;

    public TopicFilterSubscriber(TopicToken topicFilterToken, MqttQoS mqttQoS) {
        this.topicFilterToken = topicFilterToken;
        this.mqttQoS = mqttQoS;
        this.topicSubscribers = new HashMap<>();
    }

    public TopicFilterSubscriber(TopicToken topicFilterToken, MqttQoS mqttQoS, TopicSubscriber topicSubscriber) {
        this(topicFilterToken, mqttQoS);
        topicSubscribers.put(topicSubscriber.getTopic().getTopic(), topicSubscriber);
    }

    public TopicToken getTopicFilterToken() {
        return topicFilterToken;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public Map<String, TopicSubscriber> getTopicSubscribers() {
        return topicSubscribers;
    }
}
