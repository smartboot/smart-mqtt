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
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/26
 */
public class InflightQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(InflightQueue.class);
    static final AttachKey<Runnable> RETRY_TASK_ATTACH_KEY = AttachKey.valueOf("retryTask");
    private static final int TIMEOUT = 3;
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
            AckMessage ackMessage = new AckMessage(mqttMessage, id, consumer, offset);
            queue[putIndex++] = ackMessage;
            if (putIndex == queue.length) {
                putIndex = 0;
            }
            count++;

            //启动消息质量监测
            if (count == 1 && mqttMessage.getFixedHeader().getQosLevel().value() > 0) {
                retry(ackMessage);
            }
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

    /**
     * 超时重发
     */
    void retry(AckMessage ackMessage) {
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                if (ackMessage.isCommit()) {
//                    System.out.println("message has commit,ignore retry monitor");
                    return;
                }
                if (session.isDisconnect()) {
                    LOGGER.warn("session is disconnect , pause qos monitor.");
                    return;
                }
                long delay = System.currentTimeMillis() - ackMessage.getLatestTime();
                if (delay > 0) {
                    LOGGER.info("the time is not up, try again in {} milliseconds ", delay);
                    QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, delay, TimeUnit.MILLISECONDS);
                    return;
                }
                ackMessage.setLatestTime(System.currentTimeMillis());
                LOGGER.info("time out,retry...");
                switch (ackMessage.getExpectMessageType()) {
                    case PUBACK:
                    case PUBREC:
                        session.write(ackMessage.getOriginalMessage());
                        break;
                    case PUBCOMP:
                        ReasonProperties properties = null;
                        if (ackMessage.getOriginalMessage().getVersion() == MqttVersion.MQTT_5) {
                            properties = new ReasonProperties();
                        }
                        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(ackMessage.getOriginalMessage().getVariableHeader().getPacketId(), properties);
                        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
                        session.write(pubRelMessage);
                        break;
                    default:
                        throw new UnsupportedOperationException("invalid message type: " + ackMessage.getExpectMessageType());
                }
                ackMessage.setRetryCount(ackMessage.getRetryCount() + 1);
                //不断重试直至完成
                QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, TIMEOUT, TimeUnit.SECONDS);
            }
        }, TimeUnit.SECONDS.toMillis(TIMEOUT) - (System.currentTimeMillis() - ackMessage.getLatestTime()), TimeUnit.MILLISECONDS);
    }

    /**
     * 理论上该方法只会被读回调线程触发
     */
    public void notify(MqttPubQosMessage message) {
        AckMessage ackMessage = queue[(message.getVariableHeader().getPacketId() - 1) % queue.length];
        ValidateUtils.isTrue(message.getFixedHeader().getMessageType() == ackMessage.getExpectMessageType(), "invalid message type");
        ackMessage.setLatestTime(System.currentTimeMillis());
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
        if (count > 0) {
            //注册超时监听任务
            Attachment attachment = session.session.getAttachment();
            AckMessage message = queue[takeIndex];
            attachment.put(RETRY_TASK_ATTACH_KEY, () -> session.getInflightQueue().retry(message));
        }
        return ackMessage.getOffset();
    }

    public int getCount() {
        return count;
    }
}