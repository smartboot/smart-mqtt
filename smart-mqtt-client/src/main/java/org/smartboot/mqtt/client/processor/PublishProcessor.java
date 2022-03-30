package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import static org.smartboot.mqtt.common.enums.MqttQoS.AT_MOST_ONCE;

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
    public void process(MqttClientSession session, MqttPublishMessage mqttPublishMessage) {
        MqttQoS mqttQoS = mqttPublishMessage.getMqttFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(session, mqttPublishMessage);
                callBack(mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unsupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void callBack(MqttPublishMessage mqttPublishMessage) {
        byte[] payload = mqttPublishMessage.getPayload();
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

    private void processQos0(MqttClientSession session, MqttPublishMessage mqttPublishMessage) {

    }

    private void processQos1(MqttClientSession session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        sendPubAck(messageId, session);
    }

    private void processQos2(MqttClientSession session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(fixedHeader, messageId);

        session.write(pubRecMessage);
    }

    private void sendPubAck(int messageID, MqttClientSession session) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, messageID);
        session.write(pubAckMessage);
    }
}
