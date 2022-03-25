package org.smartboot.socket.mqtt.store.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.store.IMessagesStore;
import org.smartboot.socket.mqtt.store.StoredMessage;
import org.smartboot.socket.mqtt.store.SubscriberConsumeOffset;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStore implements IMessagesStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStore.class);
    private final StoredMessage[] store = new StoredMessage[64];
    private final AtomicLong putOffset = new AtomicLong(-1);

    @Override
    public void cleanTopic() {
        System.out.println("丢弃消息");
        putOffset.set(-1);
    }

    public StoredMessage poll(long offset) {
        //无数据
        if (putOffset.get() < 0) {
            return null;
        }
        //获取最新一条消息
        if (offset == SubscriberConsumeOffset.LATEST_OFFSET) {
            return store[(int) (putOffset.get() % store.length)];
        }
        //获取最早一条消息
        if (offset == SubscriberConsumeOffset.EARLIEST_OFFSET) {
            //同步锁,防止被最新数据覆盖
            synchronized (this) {
                return putOffset.get() > store.length ? store[(int) ((putOffset.get() - store.length) % store.length)] : store[0];
            }
        }

        if (offset >= latestOffset()) {
            return poll(SubscriberConsumeOffset.LATEST_OFFSET);
        }
        if (offset < latestOffset() - store.length) {
            return poll(SubscriberConsumeOffset.EARLIEST_OFFSET);
        }

        StoredMessage storedMessage = store[(int) (offset % store.length)];
        if (storedMessage.getOffset() == offset) {
            return storedMessage;
        } else {
            LOGGER.info("offset:{} data maybe overwritten, try load oldest offset", offset);
            //可能存在并发导致最新数据覆盖了当前坑位
            return poll(SubscriberConsumeOffset.EARLIEST_OFFSET);
        }
    }

    @Override
    public long latestOffset() {
        return putOffset.get();
    }

    @Override
    public synchronized void storeTopic(StoredMessage storedMessage) {
        long index = putOffset.incrementAndGet();
        storedMessage.setOffset(index);
        System.out.println("存储消息:" + index);
        store[(int) (index % store.length)] = storedMessage;
    }

}
