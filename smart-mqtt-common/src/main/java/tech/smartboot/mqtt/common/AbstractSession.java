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

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPubRecMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Hashtable;


/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public abstract class AbstractSession {

    protected String clientId;
    protected AioSession session;
    /**
     * 最近一次发送的消息
     */
    protected long latestSendMessageTime;

    /**
     * 是否正常断开连接
     */
    protected boolean disconnect = false;
    protected MqttWriter mqttWriter;

    private MqttVersion mqttVersion;

    protected InflightQueue inflightQueue;
    private final Hashtable<Integer, MqttPublishMessage> ackMessageCacheMap = new Hashtable<>();

    //消息超时重发任务
    Runnable retryRunnable;
    /**
     * 当前使用的解码器
     */
    Decoder decoder;
    // 当前正在解码的消息
    MqttMessage mqttMessage;
    // 当前正在解码的消息
    ByteBuffer disposableBuffer;

    public final void write(MqttPubRecMessage mqttMessage, MqttPublishMessage publishMessage) {
        ackMessageCacheMap.put(mqttMessage.getVariableHeader().getPacketId(), publishMessage);
        write(mqttMessage, false);
    }

    public final void notifyPubComp(int packetId) {
        MqttPublishMessage consumer = ackMessageCacheMap.remove(packetId);
        if (consumer != null) {
            accepted(consumer);
        }
    }

    protected abstract void accepted(MqttPublishMessage mqttMessage);


    public void write(MqttMessage mqttMessage, boolean autoFlush) {
        ValidateUtils.isTrue(!disconnect, "已断开连接,无法发送消息");
        try {
            mqttMessage.setVersion(mqttVersion);
            synchronized (mqttWriter) {
                mqttMessage.write(mqttWriter);
            }

            if (autoFlush) {
                mqttWriter.flush();
            }
            latestSendMessageTime = MqttUtil.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final void write(MqttMessage mqttMessage) {
        write(mqttMessage, true);
    }

    public final void flush() {
        if (!disconnect) {
            mqttWriter.flush();
        }
    }

    public final String getClientId() {
        return clientId;
    }

    public InetSocketAddress getRemoteAddress() throws IOException {
        if (disconnect) {
            throw new IOException("session is disconnect");
        }
        return session.getRemoteAddress();
    }

    /**
     * 关闭连接
     */
    public abstract void disconnect();

    public final boolean isDisconnect() {
        return disconnect;
    }

    public final MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    public final void setMqttVersion(MqttVersion mqttVersion) {
        this.mqttVersion = mqttVersion;
    }

    public final void setInflightQueue(InflightQueue inflightQueue) {
        this.inflightQueue = inflightQueue;
    }

    public final InflightQueue getInflightQueue() {
        if (disconnect) {
            throw new IllegalStateException("session is disconnect");
        }
        return inflightQueue;
    }

}
