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
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.Attachment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public abstract class AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSession.class);
    private final EventBus eventBus;
    protected String clientId;
    protected AioSession session;
    /**
     * 最近一次发送的消息
     */
    private long latestSendMessageTime;
    /**
     * 最近一次收到客户端消息的时间
     */
    private long latestReceiveMessageTime;

    /**
     * 是否正常断开连接
     */
    protected boolean disconnect = false;
    protected MqttWriter mqttWriter;

    private MqttVersion mqttVersion;

    private InflightQueue inflightQueue;
    private final Map<Integer, Runnable> ackMessageCacheMap = new ConcurrentHashMap<>();

    public AbstractSession(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public final void write(MqttPubRecMessage mqttMessage, Runnable callback) {
        ackMessageCacheMap.put(mqttMessage.getVariableHeader().getPacketId(), callback);
        write(mqttMessage, false);
    }

    public final void notifyPubComp(int packetId) {
        Runnable consumer = ackMessageCacheMap.remove(packetId);
        if (consumer != null) {
            consumer.run();
        }
    }


    public final synchronized void write(MqttMessage mqttMessage, boolean autoFlush) {
        try {
            if (disconnect) {
                this.disconnect();
                ValidateUtils.isTrue(false, "已断开连接,无法发送消息");
            }
            mqttMessage.setVersion(mqttVersion);
            eventBus.publish(EventType.WRITE_MESSAGE, EventObject.newEventObject(this, mqttMessage));

            mqttMessage.write(mqttWriter);
            if (autoFlush) {
                mqttWriter.flush();
            }
            latestSendMessageTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final void write(MqttMessage mqttMessage) {
        write(mqttMessage, true);
    }

    public synchronized void flush() {
        if (!disconnect) {
            mqttWriter.flush();
        }
    }

    public long getLatestSendMessageTime() {
        return latestSendMessageTime;
    }

    public long getLatestReceiveMessageTime() {
        return latestReceiveMessageTime;
    }

    public void setLatestReceiveMessageTime(long latestReceiveMessageTime) {
        this.latestReceiveMessageTime = latestReceiveMessageTime;
    }

    public final String getClientId() {
        return clientId;
    }

    public InetSocketAddress getRemoteAddress() throws IOException {
        return session.getRemoteAddress();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * 关闭连接
     */
    public abstract void disconnect();

    public boolean isDisconnect() {
        return disconnect;
    }

    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    public void setMqttVersion(MqttVersion mqttVersion) {
        this.mqttVersion = mqttVersion;
        Attachment attachment = session.getAttachment();
        attachment.put(MqttProtocol.MQTT_VERSION_ATTACH_KEY, mqttVersion);
    }

    public void setInflightQueue(InflightQueue inflightQueue) {
        this.inflightQueue = inflightQueue;
    }

    public InflightQueue getInflightQueue() {
        return inflightQueue;
    }

}
