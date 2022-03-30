package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.push.QosTask;
import org.smartboot.mqtt.broker.store.StoredMessage;
import org.smartboot.mqtt.broker.store.SubscriberConsumeOffset;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话，客户端和服务端之间的状态交互。
 * 一些会话持续时长与网络连接一样，另一些可以在客户端和服务端的多个连续网络连接间扩展。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSession.class);
    /**
     * 用于生成当前会话的报文标识符
     */
    private final AtomicInteger packetIdCreator = new AtomicInteger(1);
    /**
     * 当前连接订阅的Topic的消费信息
     */
    private final Map<String, SubscriberConsumeOffset> consumeOffsets = new ConcurrentHashMap<>();

    /**
     * 待响应的消息
     */
    private final Map<Integer, QosTask> qosTaskMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, StoredMessage> inboundInflightMessages = new ConcurrentHashMap<>();

    private final AioSession session;
    private final MqttContext mqttContext;
    private String clientId;
    private String username;


    public MqttSession(MqttContext mqttContext, AioSession session) {
        this.mqttContext = mqttContext;
        this.session = session;
    }


    public void write(MqttMessage mqttMessage) {
        try {
            mqttMessage.writeTo(session.writeBuffer());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        consumeOffsets.keySet().forEach(this::unsubscribe);
        mqttContext.removeSession(this);
        session.close(false);

    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void putInFightMessage(int packetId, StoredMessage storedMessage) {
        inboundInflightMessages.put(packetId, storedMessage);
    }

    public StoredMessage pollInFightMessage(int packetId) {
        return inboundInflightMessages.remove(packetId);
    }

    /*
     * 如果服务端收到一个 SUBSCRIBE 报文，
     * 报文的主题过滤器与一个现存订阅的主题过滤器相同，
     * 那么必须使用新的订阅彻底替换现存的订阅。
     * 新订阅的主题过滤器和之前订阅的相同，但是它的最大 QoS 值可以不同。
     */
    public synchronized void subscribeTopic(SubscriberConsumeOffset subscription) {
        unsubscribe(subscription.getTopic().getTopic());
        consumeOffsets.put(subscription.getTopic().getTopic(), subscription);
        subscription.getTopic().getConsumerGroup().getConsumeOffsets().put(this, subscription);
        LOGGER.info("subscribe topic:{} success, clientId:{}", subscription.getTopic(), clientId);
    }

    public void unsubscribe(String topic) {
        SubscriberConsumeOffset oldOffset = consumeOffsets.remove(topic);
        if (oldOffset != null) {
            oldOffset.setEnable(false);
            oldOffset.getTopic().getConsumerGroup().getConsumeOffsets().remove(this);
            LOGGER.info("unsubscribe topic:{} success, clientId:{}", topic, clientId);
        }
    }

    public QosTask getQosTask(int packetId) {
        return qosTaskMap.get(packetId);
    }

    public void put(QosTask qosTask) {
        qosTaskMap.put(qosTask.getPacketId(), qosTask);
    }

    public void remove(QosTask qosTask) {
        qosTaskMap.remove(qosTask.getPacketId());
    }

    public int newPacketId() {
        return packetIdCreator.getAndIncrement();
    }
}
