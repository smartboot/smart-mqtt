package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttVariableMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.protocol.MqttProtocol;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.Attachment;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    private static final int QOS0_PACKET_ID = 0;
    /**
     * req-resp 消息模式的处理回调
     */
    protected final Map<Integer, AckMessage> responseConsumers = new ConcurrentHashMap<>();
    private final QosPublisher qosPublisher;
    /**
     * 用于生成当前会话的报文标识符
     */
    private final AtomicInteger packetIdCreator = new AtomicInteger(1);
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

    public AbstractSession(QosPublisher publisher, EventBus eventBus) {
        this.qosPublisher = publisher;
        this.eventBus = eventBus;
    }

    public final synchronized void write(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> mqttMessage, Consumer<? extends MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        responseConsumers.put(mqttMessage.getVariableHeader().getPacketId(), new AckMessage(mqttMessage, consumer));
        write(mqttMessage);
    }

    public Map<Integer, AckMessage> getResponseConsumers() {
        return responseConsumers;
    }

    public final void notifyResponse(MqttVariableMessage<? extends MqttPacketIdVariableHeader> message) {
        AckMessage ackMessage = responseConsumers.remove(message.getVariableHeader().getPacketId());
        if (ackMessage != null) {
            ackMessage.setDone(true);
            ackMessage.getConsumer().accept(message);
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

            MqttCodecUtil.writeFixedHeader(mqttWriter, mqttMessage.getFixedHeader());
            mqttMessage.writeWithoutFixedHeader(mqttWriter);
            if (autoFlush) {
                mqttWriter.flush();
            }
            latestSendMessageTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final synchronized void write(MqttMessage mqttMessage) {
        write(mqttMessage, true);
    }

    public synchronized void flush() {
        if (!disconnect) {
            mqttWriter.flush();
        }
    }

    public void publish(MqttPublishMessage message, Consumer<Integer> consumer) {
        publish(message, consumer, true);
    }

    /**
     * 若发送的Qos为0，则回调的consumer packetId为0
     */
    public void publish(MqttPublishMessage message, Consumer<Integer> consumer, boolean autoFlush) {
//        LOGGER.info("publish to client:{}, topic:{} packetId:{}", clientId, message.getMqttPublishVariableHeader().topicName(), message.getMqttPublishVariableHeader().packetId());
        switch (message.getFixedHeader().getQosLevel()) {
            case AT_MOST_ONCE:
                try {
                    write(message, autoFlush);
                } finally {
                    consumer.accept(QOS0_PACKET_ID);
                }
                break;
            case AT_LEAST_ONCE:
                qosPublisher.publishQos1(this, message, consumer, autoFlush);
                break;
            case EXACTLY_ONCE:
                qosPublisher.publishQos2(this, message, consumer, autoFlush);
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

    /**
     * 生成大于0的 packetId
     */
    public int newPacketId() {
        int packageId = packetIdCreator.getAndIncrement();
        if (packageId <= 0) {
            packetIdCreator.set(0);
            return newPacketId();
        }
        if (responseConsumers.containsKey(packageId)) {
            return newPacketId();
        }
        return packageId;
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
}
