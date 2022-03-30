package org.smartboot.mqtt.broker.store;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.Topic;
import org.smartboot.mqtt.broker.push.QosTask;
import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * 订阅者的消费点位
 *
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
     * 处于消费中的消息
     */
    private final BlockingQueue<QosTask> inFightQueue = new LinkedBlockingQueue<>();
    /**
     * 服务端向客户端发送应用消息所允许的最大 QoS 等级
     */
    private final MqttQoS mqttQoS;
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

    public SubscriberConsumeOffset(Topic topic, MqttSession session, MqttQoS mqttQoS) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
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

    public BlockingQueue<QosTask> getInFightQueue() {
        return inFightQueue;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }
}
