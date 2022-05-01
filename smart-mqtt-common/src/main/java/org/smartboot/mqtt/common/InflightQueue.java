package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/26
 */
public class InflightQueue {
    private final MqttPublishMessage[] queue;
    private final long[] offsets;
    private int count;

    private int expectIndex = 0;

    public InflightQueue(int size) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new MqttPublishMessage[size];
        this.offsets = new long[size];
    }

    public int add(MqttPublishMessage mqttMessage, long offset) {
        queue[count] = mqttMessage;
        offsets[count] = offset;
        return count++;
    }

    public synchronized boolean commit(int commitIndex, Consumer<Long> consumer) {
        if (commitIndex != expectIndex) {
            //转负数表示以提交
            offsets[commitIndex] = -offsets[commitIndex];
            return false;
        }
        long offset = offsets[expectIndex++];
        while (expectIndex < count && offsets[expectIndex] < 0) {
            offset = -offsets[expectIndex];
            offsets[expectIndex] = 0;
            queue[expectIndex++] = null;
        }
        consumer.accept(offset);
        return expectIndex == count;
    }

    public void clear() {
        expectIndex = 0;
        count = 0;
    }

    public boolean isFull() {
        return count == queue.length;
    }
}
