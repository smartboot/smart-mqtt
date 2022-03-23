package org.smartboot.socket.mqtt;

import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.mqtt.message.MqttTopicSubscription;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话，客户端和服务端之间的状态交互。
 * 一些会话持续时长与网络连接一样，另一些可以在客户端和服务端的多个连续网络连接间扩展。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSession {
    /**
     * 用于生成当前会话的报文标识符
     */
    private final AtomicInteger packetIdCreator = new AtomicInteger(1);
    /**
     * 当前连接订阅的Topic集合
     */
    private final List<MqttTopicSubscription> topicSubscriptions = new ArrayList<>();
    private final AioSession session;
    private String clientId;
    private String username;


    public MqttSession(AioSession session) {
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

    public List<MqttTopicSubscription> getTopicSubscriptions() {
        return topicSubscriptions;
    }

    /*
     * 如果服务端收到一个 SUBSCRIBE 报文，
     * 报文的主题过滤器与一个现存订阅的主题过滤器相同，
     * 那么必须使用新的订阅彻底替换现存的订阅。
     * 新订阅的主题过滤器和之前订阅的相同，但是它的最大 QoS 值可以不同。
     */
    public synchronized void subscribeTopic(MqttTopicSubscription subscription) {
        topicSubscriptions.removeIf(oldSubscription -> StringUtils.equals(oldSubscription.topicName(), subscription.topicName()));
        topicSubscriptions.add(subscription);
    }

    public AtomicInteger getPacketIdCreator() {
        return packetIdCreator;
    }
}
