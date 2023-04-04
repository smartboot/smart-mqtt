package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.provider.PersistenceProvider;
import org.smartboot.mqtt.broker.provider.impl.message.PersistenceMessage;
import org.smartboot.mqtt.common.InflightMessage;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

import java.util.concurrent.Semaphore;

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

    /**
     * 最近一次订阅时间
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    private TopicToken topicFilterToken;

    private final Semaphore semaphore = new Semaphore(0);
    private boolean enable = true;

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS, long nextConsumerOffset, long retainConsumerOffset) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
        this.nextConsumerOffset = nextConsumerOffset;
        this.retainConsumerOffset = retainConsumerOffset;
    }

    public void batchPublish(BrokerContext brokerContext) {
        if (mqttSession.isDisconnect() || !enable) {
            return;
        }
        semaphore.release();
        publish0(brokerContext, 0);
        mqttSession.flush();
    }

    private void publish0(BrokerContext brokerContext, final int depth) {
        PersistenceProvider persistenceProvider = brokerContext.getProviders().getPersistenceProvider();
        PersistenceMessage persistenceMessage = persistenceProvider.get(topic.getTopic(), nextConsumerOffset);
        if (persistenceMessage == null) {
            if (semaphore.tryAcquire()) {
                topic.getQueue().offer(this);
            }
            return;
        }
        if (depth > 16) {
            if (semaphore.tryAcquire()) {
                topic.getQueue().offer(this);
                topic.getVersion().incrementAndGet();
            }
            return;
        }

        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(persistenceMessage.getPayload()).qos(mqttQoS).topicName(persistenceMessage.getTopic());
        if (mqttSession.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }

        InflightQueue inflightQueue = mqttSession.getInflightQueue();
        long offset = persistenceMessage.getOffset();
        nextConsumerOffset = offset + 1;
        brokerContext.getEventBus().publish(EventType.PUSH_PUBLISH_MESSAGE, mqttSession);
        //Qos0直接发送
        if (mqttQoS == MqttQoS.AT_MOST_ONCE) {
            mqttSession.write(publishBuilder.build(), false);
            publish0(brokerContext, depth + 1);
            return;
        }
        InflightMessage suc;
        if (depth == 0) {
            suc = inflightQueue.offer(publishBuilder, (mqtt) -> {
                //最早发送的消息若收到响应，则更新点位
                commitNextConsumerOffset(offset + 1);
                if (persistenceMessage.isRetained()) {
                    setRetainConsumerOffset(getRetainConsumerOffset() + 1);
                }
                commitRetainConsumerTimestamp(persistenceMessage.getCreateTime());
                publish0(brokerContext, 1);
            }, () -> publish0(brokerContext, 0));
        } else {
            suc = inflightQueue.offer(publishBuilder, (mqtt) -> {
                //最早发送的消息若收到响应，则更新点位
                commitNextConsumerOffset(offset + 1);
                if (persistenceMessage.isRetained()) {
                    setRetainConsumerOffset(getRetainConsumerOffset() + 1);
                }
                commitRetainConsumerTimestamp(persistenceMessage.getCreateTime());
                publish0(brokerContext, 1);
            });
        }

        // 飞行队列已满
        if (suc != null) {
            //递归处理下一个消息
            publish0(brokerContext, depth + 1);
        }

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


    public void commitRetainConsumerTimestamp(long retainConsumerTimestamp) {
        //todo
    }

    public void commitNextConsumerOffset(long nextConsumerOffset) {
        //todo
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

    public void disable() {
        this.enable = false;
    }
}
