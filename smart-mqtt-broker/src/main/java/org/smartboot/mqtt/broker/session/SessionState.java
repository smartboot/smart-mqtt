package org.smartboot.mqtt.broker.session;

import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.common.AckMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class SessionState {
    protected final Map<Integer, AckMessage> responseConsumers = new HashMap<>();
    private final List<TopicSubscriber> subscribers = new ArrayList<>();

    public Map<Integer, AckMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public List<TopicSubscriber> getSubscribers() {
        return subscribers;
    }
}
