package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.Consumer;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor extends AuthorizedMqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);


    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
//        LOGGER.info("receive publish message:{}", mqttPublishMessage);

        MqttQoS mqttQoS = mqttPublishMessage.getFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(context, session, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(context, session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(context, session, mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unsupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void processQos0(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final BrokerTopic topic = context.getOrCreateTopic(mqttPublishMessage.getVariableHeader().getTopicName());
        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        /**
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (mqttPublishMessage.getFixedHeader().isRetain()) {
            context.getProviders().getMessageStoreProvider().cleanTopic(topic.getTopic());
        }

        context.publish(topic, storedMessage);
    }

    private void processQos1(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final BrokerTopic topic = context.getOrCreateTopic(mqttPublishMessage.getVariableHeader().getTopicName());
        String clientId = session.getClientId();

        final int messageId = mqttPublishMessage.getVariableHeader().getPacketId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);

        //给 publisher 回响应
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(messageId);
        session.write(pubAckMessage);

        // 发送给subscribe
        context.publish(topic, storedMessage);

        if (mqttPublishMessage.getFixedHeader().isRetain()) {
            context.getProviders().getMessageStoreProvider().storeTopic(storedMessage);
        }
    }

    private void processQos2(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        String clientId = session.getClientId();


        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(mqttPublishMessage.getVariableHeader().getPacketId());
        //响应监听
        session.write(pubRecMessage, (Consumer<MqttPubRelMessage>) message -> {
            final BrokerTopic topic = context.getOrCreateTopic(mqttPublishMessage.getVariableHeader().getTopicName());
            StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
            storedMessage.setClientID(clientId);
            if (mqttPublishMessage.getFixedHeader().isRetain()) {

                context.getProviders().getMessageStoreProvider().storeTopic(storedMessage);
            }
            //发送pubRel消息。
            MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(message.getVariableHeader().getPacketId());
            session.write(pubRelMessage);
            // 发送给subscribe
            context.publish(topic, storedMessage);
        });
    }

    private StoredMessage asStoredMessage(MqttPublishMessage msg) {
        StoredMessage stored = new StoredMessage(msg.getPayload(), msg.getFixedHeader().getQosLevel(), msg.getVariableHeader().getTopicName());
        stored.setRetained(msg.getFixedHeader().isRetain());
        return stored;
    }

}
