package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.persistence.message.PersistenceMessage;
import org.smartboot.mqtt.broker.persistence.message.PersistenceProvider;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.util.concurrent.ExecutorService;
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
    private long retainConsumerTimestamp;

    private final Semaphore semaphore = new Semaphore(1);

    /**
     * 最近一次订阅时间
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    private TopicToken topicFilterToken;

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS, long nextConsumerOffset, long retainConsumerOffset) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
        this.nextConsumerOffset = nextConsumerOffset;
        this.retainConsumerOffset = retainConsumerOffset;
    }

    public void batchPublish(BrokerContext brokerContext, ExecutorService pushExecutor) {
        if (semaphore.availablePermits() > 1) {
            LOGGER.error("invalid semaphore:{}", semaphore.availablePermits());
        }
        if (semaphore.tryAcquire()) {
            publish0(brokerContext, pushExecutor);
        }
    }

    private void publish0(BrokerContext mqttContext, ExecutorService executorService) {
        PersistenceProvider persistenceProvider = mqttContext.getProviders().getPersistenceProvider();
        int count = 0;

        while (mqttSession.getInflightQueue().notFull()) {
            PersistenceMessage persistenceMessage = persistenceProvider.get(topic.getTopic(), nextConsumerOffset + count);
            if (persistenceMessage == null) {
                break;
            }
            count++;
            MqttPublishMessage publishMessage = MqttUtil.createPublishMessage(mqttSession.newPacketId(), persistenceMessage.getTopic(), mqttQoS, persistenceMessage.getPayload());
            InflightQueue inflightQueue = mqttSession.getInflightQueue();
            int index = inflightQueue.add(publishMessage, persistenceMessage.getOffset());
            mqttSession.publish(publishMessage, packetId -> {
                //最早发送的消息若收到响应，则更新点位
                boolean done = inflightQueue.commit(index, offset -> {
                    setNextConsumerOffset(offset + 1);
                    if (persistenceMessage.isRetained()) {
                        setRetainConsumerOffset(getRetainConsumerOffset() + 1);
                    }
                });
                if (done) {
                    setRetainConsumerTimestamp(persistenceMessage.getCreateTime());
                    inflightQueue.clear();
                    //本批次全部处理完毕
                    PersistenceMessage nextMessage = persistenceProvider.get(topic.getTopic(), getNextConsumerOffset());
                    if (nextMessage == null) {
                        semaphore.release();
                    } else {
                        executorService.execute(new AsyncTask() {
                            @Override
                            public void execute() {
                                publish0(mqttContext, executorService);
                            }
                        });

                    }
                }
            });
            mqttContext.getEventBus().publish(EventType.PUSH_PUBLISH_MESSAGE, mqttSession);
        }
        //无可publish的消息
        if (count == 0) {
            semaphore.release();
        }
        //可能此时正好有新消息投递进来
        if (mqttSession.getInflightQueue().notFull() && persistenceProvider.get(topic.getTopic(), nextConsumerOffset) != null) {
            batchPublish(mqttContext, executorService);
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

    public long getRetainConsumerTimestamp() {
        return retainConsumerTimestamp;
    }

    public void setRetainConsumerTimestamp(long retainConsumerTimestamp) {
        this.retainConsumerTimestamp = retainConsumerTimestamp;
    }

    public void setNextConsumerOffset(long nextConsumerOffset) {
        this.nextConsumerOffset = nextConsumerOffset;
    }

    public Semaphore getSemaphore() {
        return semaphore;
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
}
