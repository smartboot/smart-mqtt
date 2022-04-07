package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.client.Subscribe;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttPublishVariableHeader;

import java.util.function.Consumer;

import static org.smartboot.mqtt.common.enums.MqttQoS.AT_MOST_ONCE;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor implements MqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);

    @Override
    public void process(MqttClient session, MqttPublishMessage mqttPublishMessage) {
        MqttQoS mqttQoS = mqttPublishMessage.getMqttFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(session, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(session, mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unSupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void processQos0(MqttClient mqttClient, MqttPublishMessage mqttPublishMessage) {
        LOGGER.info("receive publish:{}", mqttPublishMessage);
        processPublishMessage(mqttPublishMessage, mqttClient);
    }

    private void processPublishMessage(MqttPublishMessage mqttPublishMessage, MqttClient mqttClient) {
        MqttPublishVariableHeader header = mqttPublishMessage.getMqttPublishVariableHeader();
        Subscribe subscribe = mqttClient.getSubscribes().get(header.topicName());
        subscribe.getConsumer().accept(mqttClient, mqttPublishMessage);
    }

    private void processQos1(MqttClient mqttClient, MqttPublishMessage mqttPublishMessage) {
        processPublishMessage(mqttPublishMessage, mqttClient);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, mqttPublishMessage.getMqttPublishVariableHeader().packetId());
        mqttClient.write(pubAckMessage);
    }

    private void processQos2(MqttClient session, MqttPublishMessage mqttPublishMessage) {
        final int messageId = mqttPublishMessage.getMqttPublishVariableHeader().packetId();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(fixedHeader, messageId);
        session.write(pubRecMessage, (Consumer<MqttPubRelMessage>) message -> {
            MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0));
            pubRelMessage.setPacketId(message.getPacketId());
            session.write(pubRelMessage);

            processPublishMessage(mqttPublishMessage, session);
        });
    }

}
