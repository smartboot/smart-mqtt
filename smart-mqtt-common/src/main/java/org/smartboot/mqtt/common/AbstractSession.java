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

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.timer.Timer;
import org.smartboot.socket.transport.AioSession;

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
    private final Hashtable<Integer, Runnable> ackMessageCacheMap = new Hashtable<>();

    protected final Timer timer;

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

    public AbstractSession(Timer timer) {
        this.timer = timer;
    }

    Timer getTimer() {
        return timer;
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


    public synchronized void write(MqttMessage mqttMessage, boolean autoFlush) {
        try {
            if (disconnect) {
//                this.disconnect();
                ValidateUtils.isTrue(false, "已断开连接,无法发送消息");
            }
            mqttMessage.setVersion(mqttVersion);
            mqttMessage.write(mqttWriter);
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

    public synchronized void flush() {
        if (!disconnect) {
            mqttWriter.flush();
        }
    }

    public final String getClientId() {
        return clientId;
    }

    public InetSocketAddress getRemoteAddress() throws IOException {
        return session.getRemoteAddress();
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
    }

    public void setInflightQueue(InflightQueue inflightQueue) {
        this.inflightQueue = inflightQueue;
    }

    public InflightQueue getInflightQueue() {
        return inflightQueue;
    }

}
