package org.smartboot.socket.mqtt.processor.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.client.MqttClient;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.*;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

import static org.smartboot.socket.mqtt.enums.MqttQoS.AT_MOST_ONCE;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);
    private MqttClient mqttClient;

    public PublishProcessor(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public void process(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        MqttQoS mqttQoS = mqttPublishMessage.getMqttFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(context, session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(context, session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(context, session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unsupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void callBack(MqttPublishMessage mqttPublishMessage) {
        byte[] payload = mqttPublishMessage.getPayload().array();
        String topicName = mqttPublishMessage.getMqttPublishVariableHeader().topicName();
        int packetId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();
        if (packetId % 10000 == 0) {
            LOGGER.info("packetId:{}", packetId);
        }
        try {
            mqttClient.callback.messageArrived(topicName, payload);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void processQos0(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {

    }

    private void processQos1(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        sendPubAck(messageId, session);
    }

    private void processQos2(MqttContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(fixedHeader, MqttPacketIdVariableHeader.from(messageId));

        session.write(pubRecMessage);
    }

    private void sendPubAck(int messageID, MqttSession session) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, MqttPacketIdVariableHeader.from(messageID));
        session.write(pubAckMessage);
    }
}
