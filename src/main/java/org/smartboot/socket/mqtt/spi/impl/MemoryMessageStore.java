package org.smartboot.socket.mqtt.spi.impl;

import org.smartboot.socket.mqtt.spi.IMessagesStore;
import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStore implements IMessagesStore {
    private final ConcurrentMap<String, Queue> stores = new ConcurrentHashMap<>();

    @Override
    public void init() {

    }

    @Override
    public Collection<StoredMessage> searchMatching(Topic key) {
        return null;
    }

    @Override
    public void cleanRetained(Topic topic) {
        System.out.println("丢弃消息");
        stores.remove(topic.getTopic());
    }

    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {
        System.out.println("存储消息");
        Queue queue = stores.computeIfAbsent(topic.getTopic(), s -> new Queue(Short.MAX_VALUE));
        queue.store[queue.putIndex++ % queue.store.length] = storedMessage;
    }

    public static class Queue {
        StoredMessage[] store;
        int putIndex;

        public Queue(int size) {
            store = new StoredMessage[size];
        }
    }
}
