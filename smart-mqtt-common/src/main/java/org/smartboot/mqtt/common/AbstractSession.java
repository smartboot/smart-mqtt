package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.listener.MqttSessionListener;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public abstract class AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSession.class);
    /**
     * req-resp 消息模式的处理回调
     */
    protected final Map<Integer, AckMessage> responseConsumers = new ConcurrentHashMap<>();
    private final QosPublisher qosPublisher;
    /**
     * 用于生成当前会话的报文标识符
     */
    private final AtomicInteger packetIdCreator = new AtomicInteger(1);
    private final List<MqttSessionListener> listeners = new ArrayList<>();
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

    public AbstractSession(QosPublisher publisher) {
        this.qosPublisher = publisher;
    }

    public final synchronized void write(MqttPacketIdentifierMessage mqttMessage, Consumer<? extends MqttPacketIdentifierMessage> consumer) {
        responseConsumers.put(mqttMessage.getVariableHeader().getPacketId(), new AckMessage(mqttMessage, consumer));
        write(mqttMessage);
    }

    public Map<Integer, AckMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public final void notifyResponse(MqttPacketIdentifierMessage message) {
        AckMessage ackMessage = responseConsumers.remove(message.getVariableHeader().getPacketId());
        ackMessage.setDone(true);
        ackMessage.getConsumer().accept(message);
    }

    public final synchronized void write(MqttMessage mqttMessage) {
        try {
            ValidateUtils.isTrue(!disconnect, "已断开连接,无法发送消息");
            listeners.forEach(listener -> listener.onMessageWrite(AbstractSession.this, mqttMessage));
            mqttMessage.writeTo(session.writeBuffer());
            session.writeBuffer().flush();
            latestSendMessageTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(MqttPublishMessage message, Consumer<Integer> consumer) {
//        LOGGER.info("publish to client:{}, topic:{} packetId:{}", clientId, message.getMqttPublishVariableHeader().topicName(), message.getMqttPublishVariableHeader().packetId());
        switch (message.getFixedHeader().getQosLevel()) {
            case AT_MOST_ONCE:
                write(message);
                break;
            case AT_LEAST_ONCE:
                qosPublisher.publishQos1(this, message, consumer);
                break;
            case EXACTLY_ONCE:
                qosPublisher.publishQos2(this, message, consumer);
                break;
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

    public int newPacketId() {
        int packageId = packetIdCreator.getAndIncrement();
        if (responseConsumers.containsKey(packageId)) {
            return newPacketId();
        }
        return packageId;
    }

    public List<MqttSessionListener> getListeners() {
        return listeners;
    }

    public void addListener(MqttSessionListener listener) {
        listeners.add(listener);
    }

    /**
     * 关闭连接
     */
    public abstract void disconnect();

    public boolean isDisconnect() {
        return disconnect;
    }


}
