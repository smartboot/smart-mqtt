package org.smartboot.mqtt.broker.plugin.provider.mock;

import org.smartboot.mqtt.broker.plugin.provider.RetainMessageProvider;
import org.smartboot.mqtt.common.StoredMessage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/30
 */
public class MockRetainMessageProvider implements RetainMessageProvider {
    private final ConcurrentHashMap<String, ArrayBlockingQueue<StoredMessage>> topicQueues = new ConcurrentHashMap<>();

    @Override
    public StoredMessage get(String topic, long startOffset, long endOffset) {
        ArrayBlockingQueue<StoredMessage> queue = topicQueues.get(topic);
        if (queue == null) {
            return null;
        }
        //内存模式采用 toArray方式效率不高
        StoredMessage[] messages = new StoredMessage[queue.size()];
        queue.toArray(messages);
        for (StoredMessage message : messages) {
            if (message.getOffset() <= startOffset) {
                continue;
            }
            if (message.getOffset() >= endOffset) {
                return null;
            }
            return message;
        }
        return null;
    }

    @Override
    public void cleanTopic(String topic) {
        topicQueues.remove(topic);
    }

    @Override
    public void storeRetainMessage(StoredMessage storedMessage) {
        ArrayBlockingQueue<StoredMessage> queue = topicQueues.computeIfAbsent(storedMessage.getTopic(), s -> new ArrayBlockingQueue<>(64));
        //队列以满，丢弃最早的消息
        if (!queue.offer(storedMessage)) {
            queue.poll();
            storeRetainMessage(storedMessage);
        }
    }
}
