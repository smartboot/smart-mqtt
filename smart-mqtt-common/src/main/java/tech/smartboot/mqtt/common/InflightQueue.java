/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import org.smartboot.socket.timer.Timer;
import tech.smartboot.mqtt.common.enums.MqttMessageType;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.message.MessageBuilder;
import tech.smartboot.mqtt.common.message.MqttFixedHeader;
import tech.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import tech.smartboot.mqtt.common.message.MqttPubRelMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.MqttSubscribeMessage;
import tech.smartboot.mqtt.common.message.MqttVariableMessage;
import tech.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import tech.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/26
 */
public class InflightQueue {
    public static final Runnable EMPTY_RUNNABLE = () -> {
    };
    private static final int TIMEOUT = 30;
    private final InflightMessage[] queue;
    private int takeIndex;
    private int putIndex;
    private volatile int count;

    private int packetId = 0;

    private final AbstractSession session;
    private final Timer timer;

    public InflightQueue(AbstractSession session, int size, Timer timer) {
        ValidateUtils.isTrue(size > 0, "inflight must >0");
        this.queue = new InflightMessage[size];
        this.session = session;
        this.timer = timer;
    }

    public CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> put(MessageBuilder publishBuilder) {
        InflightMessage inflightMessage;
        boolean flush;
        try {
            synchronized (this) {
                while (count == queue.length) {
                    this.wait();
                }
                inflightMessage = enqueue(publishBuilder);
                flush = count == queue.length;
            }
        } catch (Exception e) {
            throw new MqttException("put message into inflight queue exception", e);
        }
        session.write(inflightMessage.getOriginalMessage(), flush);
        return inflightMessage.getFuture();
    }

    public CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> offer(MessageBuilder publishBuilder) {
        return offer(publishBuilder, EMPTY_RUNNABLE);
    }

    public int available() {
        return queue.length - count;
    }

    public CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> offer(MessageBuilder publishBuilder, Runnable runnable) {
        InflightMessage inflightMessage = null;
        boolean flush;
        synchronized (this) {
            if (count == queue.length) {
                int i = putIndex - 1;
                if (i < 0) {
                    i = queue.length - 1;
                }
                queue[i].getFuture().thenRun(runnable);
            } else {
                inflightMessage = enqueue(publishBuilder);
            }
            flush = count == queue.length;
        }
        if (inflightMessage != null) {
            session.write(inflightMessage.getOriginalMessage(), flush);
            return inflightMessage.getFuture();
        }
        return null;
    }


    private InflightMessage enqueue(MessageBuilder publishBuilder) {
        int id = ++packetId;
        // 16位无符号最大值65535
        if (id > 65535) {
            id = id % queue.length + queue.length;
            packetId = id;
        }
        MqttPacketIdentifierMessage mqttMessage = publishBuilder.packetId(id).build();
        InflightMessage inflightMessage = new InflightMessage(id, mqttMessage);
        queue[putIndex++] = inflightMessage;
        if (putIndex == queue.length) {
            putIndex = 0;
        }
        count++;

        //启动消息质量监测
        if (count == 1) {
            retry(inflightMessage);
        }
        return inflightMessage;
    }

    /**
     * 超时重发
     */
    private void retry(InflightMessage inflightMessage) {
        if (inflightMessage.isCommit() || session.isDisconnect()) {
            return;
        }
        timer.schedule(new AsyncTask() {
            @Override
            public void execute() {
                if (inflightMessage.isCommit()) {
//                    System.out.println("message has commit,ignore retry monitor");
                    return;
                }
                if (session.session.isInvalid()) {
//                    LOGGER.debug("session is disconnect , pause qos monitor.");
                    return;
                }
                long delay = TimeUnit.SECONDS.toMillis(TIMEOUT) - MqttUtil.currentTimeMillis() + inflightMessage.getLatestTime();
                if (delay > 0) {
//                    LOGGER.info("the time is not up, try again in {} milliseconds ", delay);
                    timer.schedule(this, delay, TimeUnit.MILLISECONDS);
                    return;
                }
                inflightMessage.setLatestTime(MqttUtil.currentTimeMillis());
//                LOGGER.info("message:{} time out,retry...", inflightMessage.getExpectMessageType());
                switch (inflightMessage.getExpectMessageType()) {
                    case PUBACK:
                    case PUBREC:
                        MqttPublishMessage mqttMessage = (MqttPublishMessage) inflightMessage.getOriginalMessage();
                        MqttFixedHeader mqttFixedHeader = MqttFixedHeader.getInstance(mqttMessage.getFixedHeader().getMessageType(), true, mqttMessage.getFixedHeader().getQosLevel().value(), mqttMessage.getFixedHeader().isRetain());
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
                timer.schedule(this, TIMEOUT, TimeUnit.SECONDS);
            }
        }, TimeUnit.SECONDS.toMillis(TIMEOUT) - (MqttUtil.currentTimeMillis() - inflightMessage.getLatestTime()), TimeUnit.MILLISECONDS);
    }

    /**
     * 理论上该方法只会被读回调线程触发
     */
    public void notify(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message) {
        InflightMessage inflightMessage = queue[(message.getVariableHeader().getPacketId() - 1) % queue.length];
        if (inflightMessage == null) {
//            LOGGER.info("ignore duplicate message");
            return;
        }
        switch (message.getFixedHeader().getMessageType()) {
            case SUBACK:
            case UNSUBACK:
            case PUBACK:
            case PUBCOMP: {
                if (message.getFixedHeader().getMessageType() != inflightMessage.getExpectMessageType() || message.getVariableHeader().getPacketId() != inflightMessage.getAssignedPacketId()) {
//                    LOGGER.info("maybe dup ack,message:{} {} ,except:{} {}", message.getFixedHeader().getMessageType(), message.getVariableHeader().getPacketId(),
//                            inflightMessage.getExpectMessageType(), inflightMessage.getAssignedPacketId());
                    break;
                }
                inflightMessage.setResponseMessage(message);
                inflightMessage.setLatestTime(MqttUtil.currentTimeMillis());

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
                inflightMessage.setLatestTime(MqttUtil.currentTimeMillis());
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
        ValidateUtils.isTrue(originalMessage.getFixedHeader().getQosLevel().value() == 0 || originalMessage.getVariableHeader().getPacketId() == inflightMessage.getAssignedPacketId(), "invalid " + "message");
        inflightMessage.setCommit(true);

        if ((inflightMessage.getAssignedPacketId() - 1) % queue.length != takeIndex) {
            return;
        }
        if (count < queue.length) {
            this.notifyAll();
        }
        queue[takeIndex++] = null;
        count--;

        if (takeIndex == queue.length) {
            takeIndex = 0;
        }
        inflightMessage.getFuture().complete(inflightMessage.getResponseMessage());
        while (count > 0 && queue[takeIndex].isCommit()) {
            inflightMessage = queue[takeIndex];
            queue[takeIndex++] = null;
            if (takeIndex == queue.length) {
                takeIndex = 0;
            }
            count--;
            inflightMessage.getFuture().complete(inflightMessage.getResponseMessage());
        }
        if (count > 0) {
            //注册超时监听任务
            InflightMessage monitorMessage = queue[takeIndex];
            session.retryRunnable = () -> session.getInflightQueue().retry(monitorMessage);
        }
    }
}