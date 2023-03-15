package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventObject;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPubQosMessage;
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

    private InflightQueue inflightQueue;
    protected Map<Integer, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>>> responseConsumers = new ConcurrentHashMap<>();

    public AbstractSession(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public final synchronized void write(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> mqttMessage, Consumer<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> consumer) {
        responseConsumers.put(mqttMessage.getVariableHeader().getPacketId(), consumer);
        write(mqttMessage);
    }

    public final void notifyResponse(MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader> message) {
        if (message instanceof MqttPubQosMessage && message.getFixedHeader().getMessageType() != MqttMessageType.PUBREL) {
            inflightQueue.notify((MqttPubQosMessage) message);
        } else {
            responseConsumers.remove(message.getVariableHeader().getPacketId()).accept(message);
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

    public final synchronized void write(MqttMessage mqttMessage) {
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

    /**
     * 生成大于0的 packetId
     */
    public int newPacketId() {
        int packageId = packetIdCreator.getAndIncrement();
        if (packageId <= 0) {
            packetIdCreator.set(0);
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

    public void setInflightQueue(InflightQueue inflightQueue) {
        this.inflightQueue = inflightQueue;
    }

    public InflightQueue getInflightQueue() {
        return inflightQueue;
    }

}
