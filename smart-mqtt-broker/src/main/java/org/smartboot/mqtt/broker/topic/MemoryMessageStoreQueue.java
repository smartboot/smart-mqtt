/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStoreQueue implements MessageQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final int capacity;
    private Message[] store;
    private final int mask;

    private final AtomicLong putOffset = new AtomicLong(-1);

    public MemoryMessageStoreQueue() {
        this(1 << 7);
    }

    public MemoryMessageStoreQueue(int maxMessageQueueLength) {
        this.capacity = Integer.highestOneBit(maxMessageQueueLength);
        if (this.capacity != maxMessageQueueLength) {
            LOGGER.warn("maxMessageQueueLength:{} is not power of 2, use {} instead", maxMessageQueueLength, this.capacity);
        }
        this.store = new Message[capacity];
        mask = capacity - 1;
    }

    public void put(Message message) {
        message.setOffset(putOffset.incrementAndGet());
//        LOGGER.info("store message, offset:{}", message.getOffset());
        store[(int) (message.getOffset() & mask)] = message;
    }

    public Message get(long offset) {
//        System.out.println("offset:" + offset);
        Message storedMessage = store[(int) (offset & mask)];
        if (storedMessage != null && storedMessage.getOffset() == offset) {
            return storedMessage;
        }
        if (offset < putOffset.get()) {
//            System.out.println("skip...");
            return store[(int) (putOffset.get() & mask)];
        } else {
            return null;
        }
    }

    /**
     * 获取最近的消息点位
     *
     * @return
     */
    public long getLatestOffset() {
        return putOffset.get();
    }

    @Override
    public void clear() {
        store = new Message[capacity];
    }

    @Override
    public int capacity() {
        return capacity;
    }

}
