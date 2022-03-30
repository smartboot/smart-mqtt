package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerContextImpl;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.Topic;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.broker.store.StoredMessage;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);


    @Override
    public void process(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        LOGGER.info("receive publish message:{}", mqttPublishMessage);

        MqttQoS mqttQoS = mqttPublishMessage.getMqttFixedHeader().getQosLevel();
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
        final Topic topic = context.getOrCreateTopic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());

        /**
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (mqttPublishMessage.getMqttFixedHeader().isRetain()) {
            topic.getMessagesStore().cleanTopic();
        }

        context.publish(topic, BrokerContextImpl.asStoredMessage(mqttPublishMessage));
    }

    private void processQos1(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = context.getOrCreateTopic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();

        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);

        //给 publisher 回响应
        LOGGER.info("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, messageId);
        session.write(pubAckMessage);

        // 发送给subscribe
        context.publish(topic, BrokerContextImpl.asStoredMessage(mqttPublishMessage));

        if (mqttPublishMessage.getMqttFixedHeader().isRetain()) {
            topic.getMessagesStore().storeTopic(storedMessage);
        }
    }

    private void processQos2(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        String clientId = session.getClientId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, mqttPublishMessage.getMqttFixedHeader().getQosLevel(), false, 0);
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(fixedHeader, mqttPublishMessage.getMqttPublishVariableHeader().packetId());
        //响应监听
        session.putInFightMessage(mqttPublishMessage.getMqttPublishVariableHeader().packetId(), storedMessage);
        session.write(pubRecMessage);
    }

    private StoredMessage asStoredMessage(MqttPublishMessage msg) {
        StoredMessage stored = new StoredMessage(msg.getPayload(), msg.getMqttFixedHeader().getQosLevel(), msg.getMqttPublishVariableHeader().topicName());
        stored.setRetained(msg.getMqttFixedHeader().isRetain());
        return stored;
    }

}