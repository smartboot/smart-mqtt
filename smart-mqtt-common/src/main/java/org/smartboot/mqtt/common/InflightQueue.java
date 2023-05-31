/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttVariableMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.MqttAttachKey;
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
    static final AttachKey<Runnable> RETRY_TASK_ATTACH_KEY = AttachKey.valueOf(MqttAttachKey.RETRY_TASK);
    private static final int TIMEOUT = 3;
    private final InflightMessage[] queue;
    private int takeIndex;
    private int putIndex;
    private int count;

    private final AtomicInteger packetId = new AtomicInteger(0);

    private final AbstractSession session;
    private final ConcurrentLinkedQueue<Runnable> runnables = new ConcurrentLinkedQueue<>();

    public InflightQueue(AbstractSession session, int size) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new InflightMessage[size];
        this.session = session;
    }

    public InflightMessage offer(MqttMessageBuilders.MessageBuilder publishBuilder, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        return offer(publishBuilder, consumer, null);
    }

    public InflightMessage offer(MqttMessageBuilders.MessageBuilder publishBuilder, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer, Runnable runnable) {
        InflightMessage inflightMessage;
        synchronized (this) {
            if (count == queue.length) {
                if (runnable != null) {
                    runnables.offer(runnable);
                }
                return null;
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
            if (count == 1) {
                retry(inflightMessage);
            }
        }
        session.write(inflightMessage.getOriginalMessage(), count == queue.length);
        return inflightMessage;
    }

    /**
     * 超时重发
     */
    private void retry(InflightMessage inflightMessage) {
        if (inflightMessage.isCommit() || session.isDisconnect()) {
            return;
        }
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (inflightMessage.isCommit()) {
//                    System.out.println("message has commit,ignore retry monitor");
                    return;
                }
                if (session.session.isInvalid()) {
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
                LOGGER.info("message:{} time out,retry...", inflightMessage.getExpectMessageType());
                switch (inflightMessage.getExpectMessageType()) {
                    case PUBACK:
                    case PUBREC:
                        MqttPublishMessage mqttMessage = (MqttPublishMessage) inflightMessage.getOriginalMessage();
                        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(mqttMessage.getFixedHeader().getMessageType(), true, mqttMessage.getFixedHeader().getQosLevel(), mqttMessage.getFixedHeader().isRetain());
                        MqttPublishMessage dupMessage = new MqttPublishMessage(mqttFixedHeader, mqttMessage.getVariableHeader(), mqttMessage.getPayload().getPayload());
                        session.write(dupMessage);
                        break;
                    case PUBCOMP:
                        ReasonProperties properties = null;
                        if (inflightMessage.getOriginalMessage().getVersion() == MqttVersion.MQTT_5) {
                            properties = new ReasonProperties();
                        }
                        MqttVariableMessage<? extends MqttPacketIdVariableHeader> message = inflightMessage.getOriginalMessage();
                        MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
                        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(MqttFixedHeader.PUB_REL_HEADER_DUP, variableHeader);
                        session.write(pubRelMessage);
                        break;
                    case SUBACK:
                        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) inflightMessage.getOriginalMessage();
                        MqttSubscribeMessage dupSubscribeMessage = new MqttSubscribeMessage(MqttFixedHeader.SUBSCRIBE_HEADER_DUP, subscribeMessage.getVariableHeader(), subscribeMessage.getPayload());
                        session.write(dupSubscribeMessage);
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
        if (inflightMessage == null) {
            LOGGER.info("ignore duplicate message");
            return;
        }
        switch (message.getFixedHeader().getMessageType()) {
            case SUBACK:
            case UNSUBACK:
            case PUBACK:
            case PUBCOMP: {
                if (message.getFixedHeader().getMessageType() != inflightMessage.getExpectMessageType() || message.getVariableHeader().getPacketId() != inflightMessage.getAssignedPacketId()) {
//                    System.out.println("maybe dup ack,ignore:" + message.getFixedHeader().getMessageType());
                    break;
                }
                inflightMessage.setResponseMessage(message);
                inflightMessage.setLatestTime(System.currentTimeMillis());
                commit(inflightMessage);
                break;
            }
            case PUBREC:
                //说明此前出现过重复publish，切已经收到过REC,并发送过REL消息
                if (message.getFixedHeader().getMessageType() != inflightMessage.getExpectMessageType() || message.getVariableHeader().getPacketId() != inflightMessage.getAssignedPacketId()) {
//                    System.out.println("maybe dup pubRec,ignore");
                    break;
                }
                inflightMessage.setResponseMessage(message);
                inflightMessage.setLatestTime(System.currentTimeMillis());
                inflightMessage.setExpectMessageType(MqttMessageType.PUBCOMP);
                //todo
                ReasonProperties properties = null;
                if (message.getVersion() == MqttVersion.MQTT_5) {
                    properties = new ReasonProperties();
                }
                MqttPubQosVariableHeader variableHeader = new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId(), properties);
                MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(MqttFixedHeader.PUB_REL_HEADER, variableHeader);
                session.write(pubRelMessage, false);
                break;
            default:
                throw new RuntimeException(message.toString());
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
        inflightMessage.getConsumer().accept(inflightMessage.getResponseMessage());
        while (count > 0 && queue[takeIndex].isCommit()) {
            inflightMessage = queue[takeIndex];
            inflightMessage.getConsumer().accept(inflightMessage.getResponseMessage());
            queue[takeIndex++] = null;
            if (takeIndex == queue.length) {
                takeIndex = 0;
            }
            count--;
        }

        if (count > 0) {
            //注册超时监听任务
            Attachment attachment = session.session.getAttachment();
            InflightMessage monitorMessage = queue[takeIndex];
            attachment.put(RETRY_TASK_ATTACH_KEY, () -> session.getInflightQueue().retry(monitorMessage));
        }

        while (count < queue.length) {
            Runnable runnable = runnables.poll();
            if (runnable != null) {
                runnable.run();
            } else {
                break;
            }
        }
    }
}