package org.smartboot.socket.mqtt.push;

import org.smartboot.socket.mqtt.store.SubscriberConsumeOffset;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/27
 */
public class QosTask {
    private final int packetId;
    private final PushListener pushListener;
    private final SubscriberConsumeOffset consumeOffset;

    public QosTask(int packetId, SubscriberConsumeOffset consumeOffset, PushListener pushListener) {
        this.packetId = packetId;
        this.consumeOffset = consumeOffset;
        this.pushListener = pushListener;
    }

    public void done() {
        consumeOffset.getMqttSession().remove(this);
        consumeOffset.getInFightQueue().remove(this);
        pushListener.notify(consumeOffset);
    }

    public int getPacketId() {
        return packetId;
    }
}
