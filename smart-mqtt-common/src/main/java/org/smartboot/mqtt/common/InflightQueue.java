package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/26
 */
public class InflightQueue {
    private final MqttPublishMessage[] queue;
    private final long[] offsets;
    private int takeIndex;
    private int putIndex;
    private int count;

    public InflightQueue(int size) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new MqttPublishMessage[size];
        this.offsets = new long[size];
    }

    public synchronized int offer(MqttPublishMessage mqttMessage, long offset) {
        if (count == queue.length) {
            return -1;
        }
        queue[putIndex] = mqttMessage;
        offsets[putIndex] = offset;
        int index = putIndex++;
        if (putIndex == queue.length) {
            putIndex = 0;
        }
        count++;
        return index;
    }

    public boolean isFull() {
        return count == queue.length;
    }

    public synchronized long commit(int commitIndex) {
        if (commitIndex != takeIndex) {
            //转负数表示以提交
            offsets[commitIndex] = offsets[commitIndex] | Long.MIN_VALUE;
            return -1;
        }
        long offset = offsets[takeIndex];
        offsets[takeIndex] = 0;
        queue[takeIndex++] = null;
        count--;
        if (takeIndex == queue.length) {
            takeIndex = 0;
        }
        while (count > 0 && offsets[takeIndex] < 0) {
            offset = offsets[takeIndex] & ~Long.MIN_VALUE;
            offsets[takeIndex] = 0;
            queue[takeIndex++] = null;
            if (takeIndex == queue.length) {
                takeIndex = 0;
            }
            count--;
        }
        return offset;
    }
}