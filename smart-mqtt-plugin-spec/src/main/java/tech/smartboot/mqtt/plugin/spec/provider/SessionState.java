/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.provider;

import tech.smartboot.mqtt.common.InflightMessage;
import tech.smartboot.mqtt.common.enums.MqttQoS;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class SessionState {
    protected final Map<Integer, InflightMessage> responseConsumers = new HashMap<>();
    private final Map<String, MqttQoS> subscribers = new HashMap<>();


    public Map<Integer, InflightMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public Map<String, MqttQoS> getSubscribers() {
        return subscribers;
    }
}
