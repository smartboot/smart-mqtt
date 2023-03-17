package org.smartboot.mqtt.broker.provider.impl.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
class MemoryMessageStoreQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final PersistenceMessage[] store = new PersistenceMessage[64];
    private final AtomicLong putOffset = new AtomicLong(-1);

    public void put(MqttPublishMessage msg) {
        PersistenceMessage message = new PersistenceMessage(msg, putOffset.incrementAndGet());
//        LOGGER.info("store message, offset:{}", message.getOffset());
        store[(int) (message.getOffset() % store.length)] = message;
    }

    public PersistenceMessage get(long offset) {
        PersistenceMessage storedMessage = store[(int) (offset % store.length)];
        if (storedMessage == null) {
            return null;
        }
        return storedMessage.getOffset() == offset ? storedMessage : null;
    }

    /**
     * 获取最早的消息点位。该点位随时可能被新增的消息覆盖
     */
    public long getOldestOffset() {
        long offset = putOffset.get() - store.length;
        return offset > 0 ? offset : 0;
    }

    /**
     * 获取最近的消息点位
     *
     * @return
     */
    public long getLatestOffset() {
        return putOffset.get();
    }

}
