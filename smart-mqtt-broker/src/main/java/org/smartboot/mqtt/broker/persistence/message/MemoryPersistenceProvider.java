package org.smartboot.mqtt.broker.persistence.message;

import org.smartboot.mqtt.broker.messagebus.Message;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class MemoryPersistenceProvider implements PersistenceProvider {
    private static final MemoryMessageStoreQueue EMPTY_QUEUE = new MemoryMessageStoreQueue();
    private final ConcurrentHashMap<String, MemoryMessageStoreQueue> topicQueues = new ConcurrentHashMap<>();

    @Override
    public void doSave(Message message) {
        MemoryMessageStoreQueue queue = topicQueues.computeIfAbsent(message.getTopic(), s -> new MemoryMessageStoreQueue());
        queue.put(message);
    }

    @Override
    public void delete(String topic) {
        topicQueues.remove(topic);
    }


    @Override
    public PersistenceMessage get(String topic, long startOffset) {
        return getStoreQueue(topic).get(startOffset);
    }

    @Override
    public long getOldestOffset(String topic) {
        return getStoreQueue(topic).getOldestOffset();
    }

    @Override
    public long getLatestOffset(String topic) {
        return getStoreQueue(topic).getLatestOffset();
    }

    private MemoryMessageStoreQueue getStoreQueue(String topic) {
        MemoryMessageStoreQueue storeQueue = topicQueues.get(topic);
        return storeQueue == null ? EMPTY_QUEUE : storeQueue;
    }
}
