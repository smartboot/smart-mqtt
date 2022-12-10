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
    private int index;

    private int expectIndex = 0;

    public InflightQueue(int size) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new MqttPublishMessage[size];
        this.offsets = new long[size];
    }

    public int add(MqttPublishMessage mqttMessage, long offset) {
        queue[index] = mqttMessage;
        offsets[index] = offset;
        return index++;
    }

    public synchronized boolean commit(int commitIndex, Consumer<Long> consumer) {
        if (commitIndex != expectIndex) {
            //转负数表示以提交
            offsets[commitIndex] = -offsets[commitIndex];
            return false;
        }
        long offset = offsets[expectIndex++];
        while (expectIndex < index && offsets[expectIndex] < 0) {
            offset = -offsets[expectIndex];
            offsets[expectIndex] = 0;
            queue[expectIndex++] = null;
        }
        consumer.accept(offset);
        return expectIndex == index;
    }

    public void clear() {
        expectIndex = 0;
        index = 0;
    }

    public boolean notFull() {
        return index < queue.length;
    }
}
