package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/14
 */
public class AckMessage {
    /**
     * 原始消息
     */
    private final MqttMessage originalMessage;
    /**
     * 回调事件
     */
    private Consumer<? extends MqttPacketIdentifierMessage> consumer;

    /**
     * 执行状态
     */
    private boolean done;

    public AckMessage(MqttMessage originalMessage, Consumer<? extends MqttPacketIdentifierMessage> consumer) {
        this.originalMessage = originalMessage;
        this.consumer = consumer;
    }

    public MqttMessage getOriginalMessage() {
        return originalMessage;
    }


    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<? extends MqttPacketIdentifierMessage> consumer) {
        this.consumer = consumer;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
