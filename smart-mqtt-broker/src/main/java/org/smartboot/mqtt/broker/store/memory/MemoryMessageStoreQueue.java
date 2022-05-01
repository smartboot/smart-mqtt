package org.smartboot.mqtt.broker.store.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.store.MessageQueue;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStoreQueue implements MessageQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final StoredMessage[] store = new StoredMessage[64];
    private final AtomicLong putOffset = new AtomicLong(-1);
    private long latestOffset = -1;

    public synchronized StoredMessage put(String clientId, MqttPublishMessage msg) {
        StoredMessage stored = new StoredMessage(msg, clientId, putOffset.incrementAndGet());
        latestOffset = stored.getOffset();
        System.out.println("存储消息:" + stored.getOffset());
        store[(int) (stored.getOffset() % store.length)] = stored;
        return stored;
    }

    @Override
    public StoredMessage get(long offset) {
        return store[(int) (offset % store.length)];
    }

    @Override
    public long getLatestOffset() {
        return latestOffset;
    }

}
