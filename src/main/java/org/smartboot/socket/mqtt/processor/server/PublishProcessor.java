package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.message.MqttPubRecMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.store.StoredMessage;
import org.smartboot.socket.mqtt.common.Topic;

import java.nio.ByteBuffer;

import static org.smartboot.socket.mqtt.enums.MqttQoS.AT_MOST_ONCE;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);


    @Override
    public void process(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
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

    private void processQos0(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = context.getOrCreateTopic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();
        String username = session.getUsername();
        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);
//        context.publish2Subscribers(storedMessage, topic);

        /**
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (mqttPublishMessage.getMqttFixedHeader().isRetain()) {
            topic.getMessagesStore().cleanTopic();
        }
        try {
            topic.getMessagesStore().storeTopic(storedMessage);
        } finally {
            context.getTopicListener().notify(topic);
        }

    }

    private void processQos1(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = context.getOrCreateTopic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();

        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);
//        context.publish2Subscribers(storedMessage, topic, messageId);
        sendPubAck(messageId, session);

        try {
            topic.getMessagesStore().storeTopic(storedMessage);
        } finally {
            context.getTopicListener().notify(topic);
        }
    }

    private void processQos2(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = context.getOrCreateTopic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();
        String username = session.getUsername();

        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(fixedHeader, messageId);

        session.write(pubRecMessage);
    }

    private StoredMessage asStoredMessage(MqttPublishMessage msg) {
        // TODO ugly, too much array copy
        ByteBuffer payload = msg.getPayload();
        byte[] payloadContent = new byte[payload.remaining()];
        payload.get(payloadContent, payload.position(), payload.remaining());

        StoredMessage stored = new StoredMessage(payloadContent, msg.getMqttFixedHeader().getQosLevel(), msg.getMqttPublishVariableHeader().topicName());
        stored.setRetained(msg.getMqttFixedHeader().isRetain());
        return stored;
    }

    private void sendPubAck(int messageID, MqttSession session) {
        LOGGER.info("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, messageID);
        session.write(pubAckMessage);
    }
}
