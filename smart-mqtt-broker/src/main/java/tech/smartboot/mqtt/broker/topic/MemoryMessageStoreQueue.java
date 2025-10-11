/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic;

import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MessageQueue;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStoreQueue implements MessageQueue {
    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];
    private final int capacity;
    private volatile Message[] store = EMPTY_MESSAGE_ARRAY;
    private final int mask;

    private final AtomicLong putOffset = new AtomicLong(-1);

    public MemoryMessageStoreQueue(int maxMessageQueueLength) {
        this.capacity = Integer.highestOneBit(maxMessageQueueLength);
        mask = capacity - 1;
    }

    public void put(Message message) {
        if (store == EMPTY_MESSAGE_ARRAY) {
            synchronized (this) {
                if (store == EMPTY_MESSAGE_ARRAY) {
                    store = new Message[capacity];
                }
            }
        }
        message.setOffset(putOffset.incrementAndGet());
//        LOGGER.info("store message, offset:{}", message.getOffset());
        store[(int) (message.getOffset() & mask)] = message;
    }

    public Message get(long offset) {
        if (store == EMPTY_MESSAGE_ARRAY) {
            return null;
        }
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

    public void commit(long offset) {
        Message message = get(offset);
        if (message != null && message.getOffset() == offset && message.decrementAndGet() == 0) {
            store[(int) (message.getOffset() & mask)] = null;
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

    void clear() {
        store = EMPTY_MESSAGE_ARRAY;
    }

    @Override
    public int capacity() {
        return capacity;
    }

}
