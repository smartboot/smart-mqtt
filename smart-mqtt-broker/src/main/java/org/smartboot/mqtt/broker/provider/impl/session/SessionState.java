package org.smartboot.mqtt.broker.provider.impl.session;

import org.smartboot.mqtt.common.AckMessage;
import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class SessionState {
    protected final Map<Integer, AckMessage> responseConsumers = new HashMap<>();
    private final Map<String, MqttQoS> subscribers = new HashMap<>();


    public Map<Integer, AckMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public Map<String, MqttQoS> getSubscribers() {
        return subscribers;
    }
}
