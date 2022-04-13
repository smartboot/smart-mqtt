package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public class AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSession.class);
    protected final Map<Integer, Consumer<? extends MqttPacketIdentifierMessage>> responseConsumers = new ConcurrentHashMap<>();
    private final QosPublisher qosPublisher;
    /**
     * 用于生成当前会话的报文标识符
     */
    private final AtomicInteger packetIdCreator = new AtomicInteger(1);
    protected String clientId;
    protected AioSession session;
    /**
     * 最近一次发送的消息
     */
    private long latestSendMessageTime;
    /**
     * 最近一次收到客户端消息的时间
     */
    private long latestReceiveMessageSecondTime;

    public AbstractSession(QosPublisher publisher) {
        this.qosPublisher = publisher;
    }

    public final synchronized void write(MqttPacketIdentifierMessage mqttMessage, Consumer<? extends MqttPacketIdentifierMessage> consumer) {
        responseConsumers.put(mqttMessage.getPacketId(), consumer);
        write(mqttMessage);
    }

    public final void notifyResponse(MqttPacketIdentifierMessage message) {
        Consumer consumer = responseConsumers.get(message.getPacketId());
        consumer.accept(message);
    }

    public final synchronized void write(MqttMessage mqttMessage) {
        try {
            mqttMessage.writeTo(session.writeBuffer());
            session.writeBuffer().flush();
            latestSendMessageTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(MqttPublishMessage message, Consumer<Integer> consumer) {
        LOGGER.info("publish to client:{}, topic:{} packetId:{}", clientId, message.getMqttPublishVariableHeader().topicName(), message.getMqttPublishVariableHeader().packetId());
        switch (message.getMqttFixedHeader().getQosLevel()) {
            case AT_MOST_ONCE:
                qosPublisher.publishQos0(message, this::write);
                break;
            case AT_LEAST_ONCE:
                qosPublisher.publishQos1(responseConsumers, message.getMqttPublishVariableHeader().packetId(), message, consumer, this::write);
                break;
            case EXACTLY_ONCE:
                qosPublisher.publishQos2(responseConsumers, message.getMqttPublishVariableHeader().packetId(), message, consumer, this::write);
                break;
        }
    }

    public long getLatestSendMessageTime() {
        return latestSendMessageTime;
    }

    public long getLatestReceiveMessageSecondTime() {
        return latestReceiveMessageSecondTime;
    }

    public void setLatestReceiveMessageSecondTime(long latestReceiveMessageSecondTime) {
        this.latestReceiveMessageSecondTime = latestReceiveMessageSecondTime;
    }

    public final String getClientId() {
        return clientId;
    }

    public int newPacketId() {
        return packetIdCreator.getAndIncrement();
    }
}
