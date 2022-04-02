package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.store.IMessagesStore;
import org.smartboot.mqtt.broker.store.SubscribeTopicGroup;
import org.smartboot.mqtt.broker.store.impl.MemoryMessageStore;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.Token;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class Topic extends ToString {
    private static final Logger LOGGER = LoggerFactory.getLogger(Topic.class);
    private final String topic;
    private final SubscribeTopicGroup consumerGroup = new SubscribeTopicGroup(this);
    private final IMessagesStore messagesStore = new MemoryMessageStore();

    public Topic(String topic) {
        this.topic = topic;
    }

    public SubscribeTopicGroup getConsumerGroup() {
        return consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public IMessagesStore getMessagesStore() {
        return messagesStore;
    }

    @Override
    public String toString() {
        return topic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Topic other = (Topic) obj;

        return Objects.equals(this.topic, other.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

}
