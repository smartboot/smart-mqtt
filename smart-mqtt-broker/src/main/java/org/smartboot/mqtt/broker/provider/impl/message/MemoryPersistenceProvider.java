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

import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageQueue;
import org.smartboot.mqtt.broker.provider.PersistenceProvider;

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
    public Message get(String topic, long startOffset) {
        return get(topic).get(startOffset);
    }

    public MessageQueue get(String topic) {
        MessageQueue storeQueue = topicQueues.computeIfAbsent(topic, s -> new MemoryMessageStoreQueue());
        return storeQueue == null ? EMPTY_QUEUE : storeQueue;
    }
}
