package org.smartboot.mqtt.broker.persistence.session;

import org.smartboot.mqtt.broker.TopicFilterSubscriber;
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
    private final List<TopicFilterSubscriber> subscribers = new ArrayList<>();

    public Map<Integer, AckMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public List<TopicFilterSubscriber> getSubscribers() {
        return subscribers;
    }
}
