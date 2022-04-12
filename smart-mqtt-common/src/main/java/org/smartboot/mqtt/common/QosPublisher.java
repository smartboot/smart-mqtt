package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class QosPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(QosPublisher.class);

    public void publishQos0(MqttPublishMessage publishMessage, Consumer<MqttMessage> writeConsumer) {
        MqttQoS qos = publishMessage.getMqttFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.AT_MOST_ONCE, "qos is null");
        writeConsumer.accept(publishMessage);
    }

    public <T> void publishQos1(Map<T, Consumer<? extends MqttPacketIdentifierMessage>> responseConsumers, T cacheKey, MqttPublishMessage publishMessage, Consumer<Integer> consumer, Consumer<MqttMessage> writeConsumer) {
        MqttQoS qos = publishMessage.getMqttFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.AT_LEAST_ONCE, "qos is null");
        //至少一次
        responseConsumers.put(cacheKey, message -> {
            ValidateUtils.isTrue(message instanceof MqttPubAckMessage, "invalid message type");
            responseConsumers.remove(cacheKey);
            LOGGER.info("Qos1消息发送成功...");
            consumer.accept(publishMessage.getMqttPublishVariableHeader().packetId());
        });
        writeConsumer.accept(publishMessage);
    }

    public <T> void publishQos2(Map<T, Consumer<? extends MqttPacketIdentifierMessage>> responseConsumers, T cacheKey, MqttPublishMessage publishMessage, Consumer<Integer> consumer, Consumer<MqttMessage> writeConsumer) {
        MqttQoS qos = publishMessage.getMqttFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.EXACTLY_ONCE, "qos is null");
        //只有一次
        responseConsumers.put(cacheKey, message -> {
            ValidateUtils.isTrue(message instanceof MqttPubRecMessage, "invalid message type");
            ValidateUtils.isTrue(Objects.equals(message.getPacketId(), publishMessage.getMqttPublishVariableHeader().packetId()), "invalid packetId");
            responseConsumers.put(cacheKey, (Consumer<MqttPubCompMessage>) compMessage -> {
                LOGGER.info("Qos2消息发送成功...");
                consumer.accept(compMessage.getPacketId());
            });
            MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0));
            pubRelMessage.setPacketId(message.getPacketId());
            writeConsumer.accept(pubRelMessage);
        });
        writeConsumer.accept(publishMessage);
    }
}