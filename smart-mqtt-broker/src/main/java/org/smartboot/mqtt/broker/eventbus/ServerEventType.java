package org.smartboot.mqtt.broker.eventbus;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public class ServerEventType<T> extends EventType<T> {

    /**
     * Broker服务启动成功
     */
    public static final ServerEventType<BrokerContext> BROKER_STARTED = new ServerEventType<>("brokerStarted");

    /**
     * 停止Broker服务
     */
    public static final ServerEventType<BrokerContext> BROKER_DESTROY = new ServerEventType<>("brokerDestroy");
    /**
     * 创建MqttSession对象
     */
    public static final ServerEventType<MqttSession> SESSION_CREATE = new ServerEventType<>("sessionCreate");

    /**
     * 创建新Topic
     */
    public static final ServerEventType<BrokerTopic> TOPIC_CREATE = new ServerEventType<>("topicCreate");

    /**
     * Broker接收到客户端发送过来的消息
     */
    public static final ServerEventType<MqttPublishMessage> RECEIVE_PUBLISH_MESSAGE = new ServerEventType<>("receivePublishMessage");

    /**
     * 客户端订阅Topic
     */
    public static final ServerEventType<TopicSubscriber> SUBSCRIBE_TOPIC = new ServerEventType<>("subscribeTopic");

    /**
     * 客户端订阅Topic
     */
    public static final ServerEventType<TopicSubscriber> SUBSCRIBE_RE_TOPIC = new ServerEventType<>("subscribeReTopic");

    /**
     * 消息总线消费完成
     */
    public static final ServerEventType<BrokerTopic> MESSAGE_BUS_CONSUMED = new ServerEventType<>("messageBusProduced");

    /**
     * 客户端连接
     */
    public static final ServerEventType<EventObject<MqttConnectMessage>> CONNECT = new ServerEventType<>("connect");

    protected ServerEventType(String name) {
        super(name);
    }

    /**
     * 这行神奇的代码不要动
     *
     * @return
     */
    public static List<EventType<?>> types() {
        return EventType.types();
    }
}
