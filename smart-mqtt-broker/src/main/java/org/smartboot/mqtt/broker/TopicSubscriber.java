package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.persistence.message.PersistenceMessage;
import org.smartboot.mqtt.broker.persistence.message.PersistenceProvider;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.MqttUtil;

/**
 * Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class TopicSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicSubscriber.class);
    private final MqttSession mqttSession;
    /**
     * 定义消息主题
     */
    private final BrokerTopic topic;
    /**
     * 服务端向客户端发送应用消息所允许的最大 QoS 等级
     */
    private final MqttQoS mqttQoS;

    /**
     * 期望消费的点位
     */
    private long nextConsumerOffset;

    /**
     * retain的消费点位，防止重连后的retain消息被重复消费
     */
    private long retainConsumerOffset;
    private long retainConsumerTimestamp;

    /**
     * 最近一次订阅时间
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    private TopicToken topicFilterToken;
    private int pushVersion;

    private boolean ready = false;

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS, long nextConsumerOffset, long retainConsumerOffset) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
        this.nextConsumerOffset = nextConsumerOffset;
        this.retainConsumerOffset = retainConsumerOffset;
    }

    public void batchPublish(BrokerContext brokerContext) {
        publish0(brokerContext, 0);
    }

    private void publish0(BrokerContext brokerContext, int depth) {
        if (depth > 16) {
//            System.out.println("退出递归...");
            return;
        }
        PersistenceProvider persistenceProvider = brokerContext.getProviders().getPersistenceProvider();
        PersistenceMessage persistenceMessage = persistenceProvider.get(topic.getTopic(), nextConsumerOffset);
        if (persistenceMessage == null) {
            return;
        }
        MqttPublishMessage publishMessage = MqttUtil.createPublishMessage(mqttSession.newPacketId(), persistenceMessage.getTopic(), mqttQoS, persistenceMessage.getPayload());
        InflightQueue inflightQueue = mqttSession.getInflightQueue();
        int index = inflightQueue.offer(publishMessage, persistenceMessage.getOffset());
        // 飞行队列已满
        if (index == -1) {
//            System.out.println("queue is full...");
            return;
        }
        long start = System.currentTimeMillis();
        mqttSession.publish(publishMessage, packetId -> {
            //最早发送的消息若收到响应，则更新点位
            long offset = inflightQueue.commit(index);
            if (offset == -1) {
                return;
            }
            setNextConsumerOffset(offset + 1);
            if (persistenceMessage.isRetained()) {
                setRetainConsumerOffset(getRetainConsumerOffset() + 1);
            }
            setRetainConsumerTimestamp(persistenceMessage.getCreateTime());
            //本批次全部处理完毕
            int version = topic.getVersion().get();
            PersistenceMessage nextMessage = persistenceProvider.get(topic.getTopic(), getNextConsumerOffset());
            if (nextMessage == null) {
                pushVersion = version;
            }
        });
        long cost = System.currentTimeMillis() - start;
        if (cost > 100) {
            System.out.println("cost..." + cost);
        }
        brokerContext.getEventBus().publish(EventType.PUSH_PUBLISH_MESSAGE, mqttSession);
        //递归处理下一个消息
        publish0(brokerContext, ++depth);
    }

    public BrokerTopic getTopic() {
        return topic;
    }

    public MqttSession getMqttSession() {
        return mqttSession;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public long getNextConsumerOffset() {
        return nextConsumerOffset;
    }

    public long getRetainConsumerTimestamp() {
        return retainConsumerTimestamp;
    }

    public void setRetainConsumerTimestamp(long retainConsumerTimestamp) {
        this.retainConsumerTimestamp = retainConsumerTimestamp;
    }

    public void setNextConsumerOffset(long nextConsumerOffset) {
        this.nextConsumerOffset = nextConsumerOffset;
    }

    public long getRetainConsumerOffset() {
        return retainConsumerOffset;
    }

    public void setRetainConsumerOffset(long retainConsumerOffset) {
        this.retainConsumerOffset = retainConsumerOffset;
    }

    public long getLatestSubscribeTime() {
        return latestSubscribeTime;
    }

    public TopicToken getTopicFilterToken() {
        return topicFilterToken;
    }

    public void setTopicFilterToken(TopicToken topicFilterToken) {
        this.topicFilterToken = topicFilterToken;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public int getPushVersion() {
        return pushVersion;
    }
}
