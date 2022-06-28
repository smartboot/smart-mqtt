package org.smartboot.mqtt.broker.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.messagebus.Message;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
class MemoryMessageStoreQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final org.smartboot.mqtt.broker.persistence.Message[] store = new org.smartboot.mqtt.broker.persistence.Message[64];
    private final AtomicLong putOffset = new AtomicLong(-1);

    public void put(Message msg) {
        org.smartboot.mqtt.broker.persistence.Message message = new org.smartboot.mqtt.broker.persistence.Message(msg, putOffset.incrementAndGet());
//        LOGGER.info("store message, offset:{}", msg.getOffset());
        store[(int) (msg.getOffset() % store.length)] = message;
    }

    public org.smartboot.mqtt.broker.persistence.Message get(long offset) {
        org.smartboot.mqtt.broker.persistence.Message storedMessage = store[(int) (offset % store.length)];
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
