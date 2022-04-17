package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.store.SessionState;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.socket.transport.AioSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话，客户端和服务端之间的状态交互。
 * 一些会话持续时长与网络连接一样，另一些可以在客户端和服务端的多个连续网络连接间扩展。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSession extends AbstractSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSession.class);

    /**
     * 当前连接订阅的Topic的消费信息
     */
    private final Map<String, TopicSubscriber> subscribers = new ConcurrentHashMap<>();

    private final BrokerContext mqttContext;
    private String username;

    private boolean closed = false;
    /**
     * 已授权
     */
    private boolean authorized;
    /**
     * 遗嘱消息
     */
    private StoredMessage willMessage;

    private boolean cleanSession;

    public MqttSession(BrokerContext mqttContext, AioSession session, QosPublisher qosPublisher) {
        super((qosPublisher));
        this.mqttContext = mqttContext;
        this.session = session;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void close() {
        if (closed) {
            return;
        }
        if (isAuthorized()) {
            if (cleanSession) {
                mqttContext.getProviders().getSessionStateProvider().remove(clientId);
            } else {
                //当清理会话标志为 0 的会话连接断开之后，服务端必须将之后的 QoS 1 和 QoS 2 级别的消息保存为会话状态的一部分，
                // 如果这些消息匹配断开连接时客户端的任何订阅
                SessionState sessionState = new SessionState();
                sessionState.getResponseConsumers().putAll(responseConsumers);
                subscribers.values().forEach(topicSubscriber -> sessionState.getSubscribers().add(new TopicSubscriber(topicSubscriber.getTopic(), null, topicSubscriber.getMqttQoS())));
                mqttContext.getProviders().getSessionStateProvider().store(clientId, sessionState);
            }
        }

        if (willMessage != null) {
            //非正常中断，推送遗嘱消息
            mqttContext.publish(mqttContext.getOrCreateTopic(willMessage.getTopic()), willMessage);
        }
        subscribers.keySet().forEach(this::unsubscribe);
        boolean flag = mqttContext.removeSession(this);
        LOGGER.info("remove content session success:{}", flag);
        closed = true;
        session.close(false);
    }

    public boolean isClosed() {
        return closed;
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

    /*
     * 如果服务端收到一个 SUBSCRIBE 报文，
     * 报文的主题过滤器与一个现存订阅的主题过滤器相同，
     * 那么必须使用新的订阅彻底替换现存的订阅。
     * 新订阅的主题过滤器和之前订阅的相同，但是它的最大 QoS 值可以不同。
     */
    public synchronized void subscribeTopic(TopicSubscriber subscription) {

        unsubscribe(subscription.getTopic().getTopic());
        subscribers.put(subscription.getTopic().getTopic(), subscription);
        subscription.getTopic().getConsumeOffsets().put(this, subscription);
        LOGGER.info("subscribe topic:{} success, clientId:{}", subscription.getTopic(), clientId);
    }

    public void unsubscribe(String topic) {
        TopicSubscriber oldOffset = subscribers.remove(topic);
        if (oldOffset != null) {
            oldOffset.setEnable(false);
            oldOffset.getTopic().getConsumeOffsets().remove(oldOffset.getMqttSession());
            LOGGER.info("unsubscribe topic:{} success,oldClientId:{} ,currentClientId:{}", topic, oldOffset.getMqttSession().clientId, clientId);
        }
    }


    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public StoredMessage getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(StoredMessage willMessage) {
        this.willMessage = willMessage;
    }

    public Collection<TopicSubscriber> getSubscribers() {
        return subscribers.values();
    }
}
