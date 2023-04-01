package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttVariableMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final InflightMessage[] queue;
    private int takeIndex;
    private int putIndex;
    private int count;

    private final AtomicInteger packetId = new AtomicInteger(0);

    private final AbstractSession session;

    private final boolean skipCommit;

    private final ConcurrentLinkedQueue<PendingUnit> pendingQueue = new ConcurrentLinkedQueue();

    public InflightQueue(AbstractSession session, int size, boolean skipCommit) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new InflightMessage[size];
        this.session = session;
        this.skipCommit = skipCommit;
    }

    public boolean offer(MqttMessageBuilders.MessageBuilder publishBuilder, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        InflightMessage inflightMessage;
        synchronized (this) {
            if (count == queue.length) {
                pendingQueue.add(new PendingUnit(publishBuilder, consumer));
                return false;
            }
            int id = packetId.incrementAndGet();
            // 16位无符号最大值65535
            if (id > 65535) {
                id = id % queue.length + queue.length;
                packetId.set(id);
            }
            MqttPacketIdentifierMessage mqttMessage = publishBuilder.packetId(id).build();
            inflightMessage = new InflightMessage(id, mqttMessage, consumer);
            queue[putIndex++] = inflightMessage;
            if (putIndex == queue.length) {
                putIndex = 0;
            }
            count++;

            //启动消息质量监测
            if (count == 1 && mqttMessage.getFixedHeader().getQosLevel().value() > 0) {
                retry(inflightMessage);
            }
//        System.out.println("publish...");

        }
        session.write(inflightMessage.getOriginalMessage(), count == queue.length);
        // QOS0直接响应
        if (inflightMessage.getOriginalMessage().getFixedHeader().getQosLevel() == MqttQoS.AT_MOST_ONCE) {
            inflightMessage.setResponseMessage(inflightMessage.getOriginalMessage());
            inflightMessage.setCommit(true);
            if ((inflightMessage.getAssignedPacketId() - 1) % queue.length == takeIndex) {
                commit(inflightMessage);
            }
        }
        return true;
    }

    /**
     * 超时重发
     */
    void retry(InflightMessage inflightMessage) {
        if (inflightMessage.isCommit() || session.isDisconnect()) {
            return;
        }
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                if (inflightMessage.isCommit()) {
//                    System.out.println("message has commit,ignore retry monitor");
                    return;
                }
                if (session.isDisconnect()) {
                    LOGGER.debug("session is disconnect , pause qos monitor.");
                    return;
                }
                long delay = TimeUnit.SECONDS.toMillis(TIMEOUT) - System.currentTimeMillis() + inflightMessage.getLatestTime();
                if (delay > 0) {
                    LOGGER.info("the time is not up, try again in {} milliseconds ", delay);
                    QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, delay, TimeUnit.MILLISECONDS);
                    return;
                }
                inflightMessage.setLatestTime(System.currentTimeMillis());
                LOGGER.info("message:{} time out,retry...", inflightMessage.getOriginalMessage().getFixedHeader());
                switch (inflightMessage.getExpectMessageType()) {
                    case PUBACK:
                    case PUBREC:
                        session.write(inflightMessage.getOriginalMessage());
                        break;
                    case PUBCOMP:
                        ReasonProperties properties = null;
                        if (inflightMessage.getOriginalMessage().getVersion() == MqttVersion.MQTT_5) {
                            properties = new ReasonProperties();
                        }
                        MqttVariableMessage<? extends MqttPacketIdVariableHeader> message = inflightMessage.getOriginalMessage();
                        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
                        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
                        session.write(pubRelMessage);
                        break;
                    default:
                        throw new UnsupportedOperationException("invalid message type: " + inflightMessage.getExpectMessageType());
                }
                inflightMessage.setRetryCount(inflightMessage.getRetryCount() + 1);
                //不断重试直至完成
                QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(this, TIMEOUT, TimeUnit.SECONDS);
            }
        }, TimeUnit.SECONDS.toMillis(TIMEOUT) - (System.currentTimeMillis() - inflightMessage.getLatestTime()), TimeUnit.MILLISECONDS);
    }

    /**
     * 理论上该方法只会被读回调线程触发
     */
    public void notify(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message) {
        InflightMessage inflightMessage = queue[(message.getVariableHeader().getPacketId() - 1) % queue.length];
        ValidateUtils.isTrue(message.getFixedHeader().getMessageType() == inflightMessage.getExpectMessageType(), "invalid message type");
        ValidateUtils.isTrue(message.getVariableHeader().getPacketId() == inflightMessage.getAssignedPacketId(), "invalid message packetId");
        inflightMessage.setResponseMessage(message);
        inflightMessage.setLatestTime(System.currentTimeMillis());
        switch (message.getFixedHeader().getMessageType()) {
            case SUBACK:
            case UNSUBACK:
            case PUBACK:
            case PUBCOMP: {
                commit(inflightMessage);
                break;
            }
            case PUBREC:
                inflightMessage.setExpectMessageType(MqttMessageType.PUBCOMP);
                //todo
                ReasonProperties properties = null;
                if (message.getVersion() == MqttVersion.MQTT_5) {
                    properties = new ReasonProperties();
                }
                MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
                MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
                session.write(pubRelMessage, false);
                break;
            default:
                throw new RuntimeException();
        }
    }

    private synchronized void commit(InflightMessage inflightMessage) {
        MqttVariableMessage<? extends MqttPacketIdVariableHeader> originalMessage = inflightMessage.getOriginalMessage();
        ValidateUtils.isTrue(originalMessage.getFixedHeader().getQosLevel().value() == 0 || originalMessage.getVariableHeader().getPacketId() == inflightMessage.getAssignedPacketId(), "invalid message");
        inflightMessage.setCommit(true);

        if ((inflightMessage.getAssignedPacketId() - 1) % queue.length != takeIndex) {
            return;
        }
        queue[takeIndex++] = null;
        count--;
        if (takeIndex == queue.length) {
            takeIndex = 0;
        }
        if (!skipCommit) {
            inflightMessage.getConsumer().accept(inflightMessage.getResponseMessage());
        }
        while (count > 0 && queue[takeIndex].isCommit()) {
            inflightMessage = queue[takeIndex];
            if (!skipCommit) {
                inflightMessage.getConsumer().accept(inflightMessage.getResponseMessage());
            }
            queue[takeIndex++] = null;
            if (takeIndex == queue.length) {
                takeIndex = 0;
            }
            count--;
        }
        PendingUnit pendingUnit;
        while ((pendingUnit = pendingQueue.poll()) != null) {
            if (!offer(pendingUnit.publishBuilder, pendingUnit.consumer)) {
                break;
            }
        }
        if (skipCommit) {
            inflightMessage.getConsumer().accept(inflightMessage.getResponseMessage());
        }
        if (count > 0) {
            //注册超时监听任务
            Attachment attachment = session.session.getAttachment();
            InflightMessage monitorMessage = queue[takeIndex];
            attachment.put(RETRY_TASK_ATTACH_KEY, () -> session.getInflightQueue().retry(monitorMessage));
        }
    }

    class PendingUnit {
        MqttMessageBuilders.MessageBuilder publishBuilder;
        Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer;

        public PendingUnit(MqttMessageBuilders.MessageBuilder publishBuilder, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
            this.publishBuilder = publishBuilder;
            this.consumer = consumer;
        }
    }
}