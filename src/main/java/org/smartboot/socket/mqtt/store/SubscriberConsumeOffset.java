package org.smartboot.socket.mqtt.store;

import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.common.Topic;

import java.util.concurrent.Semaphore;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class SubscriberConsumeOffset {
    /**
     * 最早的消息点位
     */
    public static final long EARLIEST_OFFSET = -1;
    /**
     * 最新的消息点位
     */
    public static final long LATEST_OFFSET = -2;
    private final MqttSession mqttSession;
    private final Semaphore pushSemaphore = new Semaphore(1);
    /**
     * 定义消息主题
     */
    private final Topic topic;
    /**
     * 上一个消费点位
     */
    private long lastOffset = -1;
    /**
     * 下一个消费点位
     */
    private long nextOffset = LATEST_OFFSET;

    /**
     * 是否可用
     */
    private boolean enable = true;

    public SubscriberConsumeOffset(Topic topic, MqttSession session) {
        this.topic = topic;
        this.mqttSession = session;
    }

    public Topic getTopic() {
        return topic;
    }

    public long getNextOffset() {
        return nextOffset;
    }

    public void setNextOffset(long nextOffset) {
        this.nextOffset = nextOffset;
    }

    public MqttSession getMqttSession() {
        return mqttSession;
    }

    public Semaphore getPushSemaphore() {
        return pushSemaphore;
    }

    public long getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(long lastOffset) {
        this.lastOffset = lastOffset;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
