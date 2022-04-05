package org.smartboot.mqtt.broker.store.memory;

import org.smartboot.mqtt.broker.plugin.provider.MessageStoreProvider;
import org.smartboot.mqtt.common.StoredMessage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public class MemoryMessageStoreProvider implements MessageStoreProvider {
    private static final MemoryMessageStoreQueue EMPTY_QUEUE = new MemoryMessageStoreQueue();
    private final ConcurrentHashMap<String, MemoryMessageStoreQueue> topicQueues = new ConcurrentHashMap<>();

    @Override
    public MemoryMessageStoreQueue getStoreQueue(String topic) {
        MemoryMessageStoreQueue storeQueue = topicQueues.get(topic);
        return storeQueue == null ? EMPTY_QUEUE : storeQueue;
    }

    @Override
    public void cleanTopic(String topic) {
        topicQueues.remove(topic);
    }

    @Override
    public void storeTopic(StoredMessage storedMessage) {
        MemoryMessageStoreQueue queue = topicQueues.computeIfAbsent(storedMessage.getTopic(), s -> new MemoryMessageStoreQueue());
        queue.put(storedMessage);
    }
}
