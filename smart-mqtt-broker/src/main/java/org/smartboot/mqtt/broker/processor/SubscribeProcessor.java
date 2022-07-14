package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.plugin.provider.TopicTokenUtil;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubAckPayload;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;

import java.util.function.Consumer;

/**
 * 客户端订阅消息
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SubscribeProcessor extends AuthorizedMqttProcessor<MqttSubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeProcessor.class);

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttSubscribeMessage mqttSubscribeMessage) {
//        LOGGER.info("receive subscribe message:{}", mqttSubscribeMessage);

        //有效载荷包含一个返回码清单。每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        int[] qosArray = new int[mqttSubscribeMessage.getMqttSubscribePayload().getTopicSubscriptions().size()];
        int i = 0;
        for (MqttTopicSubscription mqttTopicSubscription : mqttSubscribeMessage.getMqttSubscribePayload().getTopicSubscriptions()) {
            TopicToken topicToken = new TopicToken(mqttTopicSubscription.getTopicFilter());
            match(session, topicToken, context, topic -> {
                LOGGER.info("topicFilter:{} subscribe topic:{} success!", mqttTopicSubscription.getTopicFilter(), topic.getTopic());
                long latestOffset = context.getProviders().getPersistenceProvider().getLatestOffset(topic.getTopic());
                long retainOldestOffset = context.getProviders().getRetainMessageProvider().getOldestOffset(topic.getTopic());
                //以当前消息队列的最新点位为起始点位
                TopicSubscriber consumeOffset = new TopicSubscriber(topic, session, mqttTopicSubscription.getQualityOfService(), latestOffset + 1, retainOldestOffset);
                consumeOffset.setTopicFilterToken(topicToken);
                session.subscribeTopic(consumeOffset);

                context.getEventBus().publish(ServerEventType.SUBSCRIBE_TOPIC, consumeOffset);
            });
            qosArray[i++] = mqttTopicSubscription.getQualityOfService().value();
        }


        //订阅确认
        //允许服务端在发送 SUBACK 报文之前就开始发送与订阅匹配的 PUBLISH 报文
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(mqttSubscribeMessage.getVariableHeader().getPacketId());

        //有效载荷包含一个返回码清单。
        // 每个返回码对应等待确认的 SUBSCRIBE 报文中的一个主题过滤器。
        // 返回码的顺序必须和 SUBSCRIBE 报文中主题过滤器的顺序相同
        mqttSubAckMessage.setMqttSubAckPayload(new MqttSubAckPayload(qosArray));
        session.write(mqttSubAckMessage);
    }

    public void match(MqttSession session, TopicToken topicToken, BrokerContext context, Consumer<BrokerTopic> consumer) {
        //精准匹配
        if (!topicToken.isWildcards()) {
            BrokerTopic topic = context.getOrCreateTopic(topicToken.getTopicFilter());//可能会先触发TopicFilterSubscriber.subscribe
            consumer.accept(topic);
            return;
        }

        //通配符匹配存量Topic
        for (BrokerTopic topic : context.getTopics()) {
            if (TopicTokenUtil.match(topic.getTopicToken(), topicToken)) {
                consumer.accept(topic);
            }
        }

        //通配符匹配增量Topic
        context.getEventBus().subscribe(ServerEventType.TOPIC_CREATE, new EventBusSubscriber<>() {
            @Override
            public boolean enable() {
                return !session.isDisconnect();
            }

            @Override
            public void subscribe(EventType<BrokerTopic> eventType, BrokerTopic object) {
                if (TopicTokenUtil.match(object.getTopicToken(), topicToken)) {
                    consumer.accept(object);
                }
            }
        });
    }

}
