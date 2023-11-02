/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.topic.BrokerTopic;

/**
 * Topic订阅
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/28
 */
public interface SubscribeProvider {
    default boolean subscribeTopic(String topicFilter, MqttSession session) {
        //4.7.2 应用不能使用 $ 字符开头的主题
        return !topicFilter.startsWith("$");
    }

    default boolean matchTopic(BrokerTopic brokerTopic, MqttSession session) {
        return !brokerTopic.getTopic().startsWith("$SYS/");
    }

}
