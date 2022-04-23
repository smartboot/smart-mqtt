package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttMessage;
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
        MqttQoS qos = publishMessage.getFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.AT_MOST_ONCE, "qos is null");
        writeConsumer.accept(publishMessage);
    }

    public <T> void publishQos1(Map<T, AckMessage> responseConsumers, T cacheKey, MqttPublishMessage publishMessage, Consumer<Integer> consumer, Consumer<MqttMessage> writeConsumer) {
        MqttQoS qos = publishMessage.getFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.AT_LEAST_ONCE, "qos is null");
        //至少一次
        responseConsumers.put(cacheKey, new AckMessage(publishMessage, message -> {
            ValidateUtils.isTrue(message instanceof MqttPubAckMessage, "invalid message type");
            responseConsumers.remove(cacheKey);
            LOGGER.info("Qos1消息发送成功...");
            consumer.accept(publishMessage.getVariableHeader().getPacketId());
        }));
        writeConsumer.accept(publishMessage);
    }

    public <T> void publishQos2(Map<T, AckMessage> responseConsumers, T cacheKey, MqttPublishMessage publishMessage, Consumer<Integer> consumer, Consumer<MqttMessage> writeConsumer) {
        MqttQoS qos = publishMessage.getFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.EXACTLY_ONCE, "qos is null");
        //只有一次
        responseConsumers.put(cacheKey, new AckMessage(publishMessage, message -> {
            ValidateUtils.isTrue(message instanceof MqttPubRecMessage, "invalid message type");
            ValidateUtils.isTrue(Objects.equals(message.getVariableHeader().getPacketId(), publishMessage.getVariableHeader().getPacketId()), "invalid packetId");
            MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(message.getVariableHeader().getPacketId());
            responseConsumers.put(cacheKey, new AckMessage(pubRelMessage, compMessage -> {
                ValidateUtils.isTrue(compMessage instanceof MqttPubCompMessage, "invalid message type");
                ValidateUtils.isTrue(Objects.equals(compMessage.getVariableHeader().getPacketId(), pubRelMessage.getVariableHeader().getPacketId()), "invalid packetId");
                LOGGER.info("Qos2消息发送成功...");
                consumer.accept(compMessage.getVariableHeader().getPacketId());
            }));

            writeConsumer.accept(pubRelMessage);
        }));
        writeConsumer.accept(publishMessage);
    }
}