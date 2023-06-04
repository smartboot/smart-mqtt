/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.provider.PersistenceProvider;
import org.smartboot.mqtt.broker.provider.impl.message.PersistenceMessage;
import org.smartboot.mqtt.common.InflightQueue;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

import java.util.concurrent.CompletableFuture;
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
    private MqttQoS mqttQoS;

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
        publishAvailable(brokerContext);
        mqttSession.flush();
    }

    private void publishAvailable(BrokerContext brokerContext) {
        PersistenceProvider persistenceProvider = brokerContext.getProviders().getPersistenceProvider();
        PersistenceMessage persistenceMessage = persistenceProvider.get(topic.getTopic(), nextConsumerOffset);
        if (persistenceMessage == null) {
            if (semaphore.tryAcquire()) {
                topic.getQueue().offer(this);
                if (persistenceProvider.get(topic.getTopic(), nextConsumerOffset) != null) {
                    topic.getVersion().incrementAndGet();
                    brokerContext.getEventBus().publish(ServerEventType.NOTIFY_TOPIC_PUSH, topic);
                }
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
            publishAvailable(brokerContext);
            return;
        }

        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = inflightQueue.offer(publishBuilder);
        if (future == null) {
            return;
        }
        future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
            //最早发送的消息若收到响应，则更新点位
            commitNextConsumerOffset(offset + 1);
            if (persistenceMessage.isRetained()) {
                setRetainConsumerOffset(getRetainConsumerOffset() + 1);
            }
            commitRetainConsumerTimestamp(persistenceMessage.getCreateTime());
            publishAvailable(brokerContext);
        });

        publishAvailable(brokerContext);
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

    public void setMqttQoS(MqttQoS mqttQoS) {
        this.mqttQoS = mqttQoS;
    }
}
