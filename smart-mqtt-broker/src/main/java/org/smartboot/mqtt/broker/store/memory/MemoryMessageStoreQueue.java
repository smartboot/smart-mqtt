package org.smartboot.mqtt.broker.store.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.store.MessageQueue;
import org.smartboot.mqtt.common.StoredMessage;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStoreQueue implements MessageQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final StoredMessage[] store = new StoredMessage[64];
    private final AtomicLong putOffset = new AtomicLong(-1);

    public synchronized void forEach(Consumer<StoredMessage> consumer) {
        int startIndex = putOffset.get() < store.length ? 0 : (int) (putOffset.get() - store.length);
        while (startIndex <= putOffset.get()) {
            consumer.accept(store[startIndex++ % store.length]);
        }
    }

    public synchronized void put(StoredMessage storedMessage) {
        long index = putOffset.incrementAndGet();
        storedMessage.setOffset(index);
        System.out.println("存储消息:" + index);
        store[(int) (index % store.length)] = storedMessage;
    }

}
