/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec;

import tech.smartboot.mqtt.common.TopicToken;
import tech.smartboot.mqtt.common.enums.MqttQoS;

/**
 * @author 三刀
 * @version v1.0 4/15/25
 */
public interface MessageDeliver {
    MqttSession getMqttSession();

    BrokerTopic getTopic();


    TopicToken getTopicFilterToken();

    MqttQoS getMqttQoS();

}
