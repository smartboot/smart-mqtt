package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPubQosMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/26
 */
public class InflightQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(InflightQueue.class);
    private final AckMessage[] queue;
    private int takeIndex;
    private int putIndex;
    private int count;

    private final AtomicInteger packetId = new AtomicInteger(0);

    private final AbstractSession session;

    public InflightQueue(AbstractSession session, int size) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new AckMessage[size];
        this.session = session;
    }

    public boolean offer(MqttMessageBuilders.PublishBuilder publishBuilder, Consumer<Long> consumer, long offset) {
        int id = 0;
        MqttPublishMessage mqttMessage;
        synchronized (this) {
            if (count == queue.length) {
                return false;
            }
            id = packetId.incrementAndGet();
            // 16位无符号最大值65535
            if (id > 65535) {
                id = id % queue.length + queue.length;
                packetId.set(id);
            }
            publishBuilder.packetId(id);
            mqttMessage = publishBuilder.build();
            queue[putIndex++] = new AckMessage(mqttMessage, id, consumer, offset);
            if (putIndex == queue.length) {
                putIndex = 0;
            }
            count++;
//        System.out.println("publish...");

        }
        session.write(mqttMessage, false);
        // QOS直接响应
        if (mqttMessage.getFixedHeader().getQosLevel() == MqttQoS.AT_MOST_ONCE) {
            long offset1 = commit(id);
//            ValidateUtils.isTrue(offset1 == -1 || offset1 == offset, "invalid offset");
            consumer.accept(offset);
        }
        return true;
    }

    public void notify(MqttPubQosMessage message) {
        AckMessage ackMessage = queue[(message.getVariableHeader().getPacketId() - 1) % queue.length];
        ValidateUtils.isTrue(message.getFixedHeader().getMessageType() == ackMessage.getExpectMessageType(), "invalid message type");
        switch (message.getFixedHeader().getMessageType()) {
            case PUBACK: {
                long offset = commit(message.getVariableHeader().getPacketId());
                ackMessage.getConsumer().accept(offset);
                break;
            }
            case PUBREC:
                ackMessage.setExpectMessageType(MqttMessageType.PUBCOMP);
                //todo
                ReasonProperties properties = null;
                if (message.getVersion() == MqttVersion.MQTT_5) {
                    properties = new ReasonProperties();
                }
                MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
                MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
                session.write(pubRelMessage, false);
                break;
            case PUBCOMP:
                long offset = commit(message.getVariableHeader().getPacketId());
                ackMessage.getConsumer().accept(offset);
                break;
            default:
                throw new RuntimeException();
        }
    }

    private synchronized long commit(int packetId) {
        int commitIndex = (packetId - 1) % queue.length;
        AckMessage ackMessage = queue[commitIndex];
        ValidateUtils.isTrue(ackMessage.getPacketId() == packetId, "invalid message");
        ackMessage.setCommit(true);

        if (commitIndex != takeIndex) {
            return -1;
        }
        queue[takeIndex++] = null;
        count--;
        if (takeIndex == queue.length) {
            takeIndex = 0;
        }
        while (count > 0 && queue[takeIndex].isCommit()) {
            ackMessage = queue[takeIndex];
            queue[takeIndex++] = null;
            if (takeIndex == queue.length) {
                takeIndex = 0;
            }
            count--;
        }
        return ackMessage.getOffset();
    }

    public int getCount() {
        return count;
    }
}