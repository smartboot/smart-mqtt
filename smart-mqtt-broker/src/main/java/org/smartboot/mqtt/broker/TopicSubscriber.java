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
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.broker.provider.PersistenceProvider;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * 最近一次订阅时间
     */
    private final long latestSubscribeTime = System.currentTimeMillis();

    private TopicToken topicFilterToken;

    private final AtomicBoolean semaphore = new AtomicBoolean(false);

    private boolean enable = true;

    TopicSubscriber() {
        topic = null;
        mqttSession = null;
    }

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS, long nextConsumerOffset) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
        this.nextConsumerOffset = nextConsumerOffset;
        session.getEventBus().publish(ServerEventType.SUBSCRIBE_TOPIC, this);
    }

    public void batchPublish(BrokerContext brokerContext) {
        if (mqttSession.isDisconnect() || !enable) {
            return;
        }
        if (semaphore.compareAndSet(false, true)) {
            publishAvailable(brokerContext);
            mqttSession.flush();
        }
    }

    private void publishAvailable(BrokerContext brokerContext) {
        PersistenceProvider persistenceProvider = brokerContext.getProviders().getPersistenceProvider();
        Message message = persistenceProvider.get(topic.getTopic(), nextConsumerOffset);
        if (message == null) {
            if (semaphore.compareAndSet(true, false)) {
                topic.getQueue().offer(this);
                if (persistenceProvider.get(topic.getTopic(), nextConsumerOffset) != null) {
                    topic.getVersion().incrementAndGet();
                    brokerContext.getEventBus().publish(ServerEventType.NOTIFY_TOPIC_PUSH, topic);
                }
            }
            return;
        }

        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(message.getPayload()).qos(mqttQoS).topicName(message.getTopic());
        if (mqttSession.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }

        long offset = message.getOffset();
        nextConsumerOffset = offset + 1;
        //Qos0直接发送
        if (mqttQoS == MqttQoS.AT_MOST_ONCE) {
            mqttSession.write(publishBuilder.build(), false);
            publishAvailable(brokerContext);
            return;
        }

        CompletableFuture<MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> future = mqttSession.getInflightQueue().offer(publishBuilder, mqttPacketIdentifierMessage -> {
            if (semaphore.compareAndSet(true, false)) {
                topic.getQueue().offer(TopicSubscriber.this);
                topic.getVersion().incrementAndGet();
            }
            brokerContext.getEventBus().publish(ServerEventType.NOTIFY_TOPIC_PUSH, topic);
        });
        if (future == null) {
            return;
        }
        future.whenComplete((mqttPacketIdentifierMessage, throwable) -> {
            //最早发送的消息若收到响应，则更新点位
            commitNextConsumerOffset(offset + 1);
            commitRetainConsumerTimestamp(message.getCreateTime());
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
        mqttSession.getEventBus().publish(ServerEventType.UNSUBSCRIBE_TOPIC, this);
    }

    public void setMqttQoS(MqttQoS mqttQoS) {
        this.mqttQoS = mqttQoS;
    }
}
