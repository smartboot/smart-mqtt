package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttMessageIdVariableHeader;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.message.MqttPubrecMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.spi.IMessagesStore;
import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;
import org.smartboot.socket.mqtt.spi.impl.MemoryMessageStore;

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
    private IMessagesStore messagesStore = new MemoryMessageStore();

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
        final Topic topic = new Topic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();
        String username = session.getUsername();
        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);
        context.publish2Subscribers(storedMessage, topic);

        if (mqttPublishMessage.getMqttFixedHeader().isRetain()) {
            messagesStore.cleanRetained(topic);
        }
    }

    private void processQos1(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = new Topic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();
        String username = session.getUsername();

        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);
        context.publish2Subscribers(storedMessage, topic, messageId);
        sendPubAck(messageId, session);

        if (mqttPublishMessage.getMqttFixedHeader().isRetain()) {
            if (mqttPublishMessage.getPayload().isReadOnly()) {
                messagesStore.storeRetained(topic, storedMessage);
            } else {
                messagesStore.cleanRetained(topic);
            }
        }
    }

    private void processQos2(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final Topic topic = new Topic(mqttPublishMessage.getMqttPublishVariableHeader().topicName());
        String clientId = session.getClientId();
        String username = session.getUsername();

        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        StoredMessage storedMessage = asStoredMessage(mqttPublishMessage);
        storedMessage.setClientID(clientId);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttPubrecMessage pubRecMessage = new MqttPubrecMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageId));

        session.write(pubRecMessage);
    }

    private StoredMessage asStoredMessage(MqttPublishMessage msg) {
        // TODO ugly, too much array copy
        ByteBuffer payload = msg.getPayload();
        byte[] payloadContent = new byte[payload.remaining()];
        payload.get(payloadContent, payload.position(), payload.remaining());

        StoredMessage stored = new StoredMessage(payloadContent,
                msg.getMqttFixedHeader().getQosLevel(), msg.getMqttPublishVariableHeader().topicName());
        stored.setRetained(msg.getMqttFixedHeader().isRetain());
        return stored;
    }

    private void sendPubAck(int messageID, MqttSession session) {
        LOGGER.info("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageID));
        session.write(pubAckMessage);
    }
}
