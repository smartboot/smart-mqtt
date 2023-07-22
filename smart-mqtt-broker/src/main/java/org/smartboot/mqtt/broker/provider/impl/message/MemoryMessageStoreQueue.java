/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider.impl.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
class MemoryMessageStoreQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMessageStoreQueue.class);
    private final PersistenceMessage[] store = new PersistenceMessage[64];
    private final AtomicLong putOffset = new AtomicLong(-1);

    public void put(PersistenceMessage message) {
        message.setOffset(putOffset.incrementAndGet());
//        LOGGER.info("store message, offset:{}", message.getOffset());
        store[(int) (message.getOffset() % store.length)] = message;
    }

    public PersistenceMessage get(long offset) {
        PersistenceMessage storedMessage = store[(int) (offset % store.length)];
        if (storedMessage != null && storedMessage.getOffset() == offset) {
            return storedMessage;
        }
        if (offset < putOffset.get()) {
//            System.out.println("skip...");
            return store[(int) (putOffset.get() % store.length)];
        } else {
            return null;
        }
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
