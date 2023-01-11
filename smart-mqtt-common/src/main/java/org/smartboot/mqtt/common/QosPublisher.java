package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class QosPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(QosPublisher.class);

    void publishQos1(AbstractSession session, MqttPublishMessage publishMessage, Consumer<Integer> consumer, boolean autoFlush) {
        MqttQoS qos = publishMessage.getFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.AT_LEAST_ONCE, "qos is null");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer cacheKey = publishMessage.getVariableHeader().getPacketId();
        //至少一次

        session.responseConsumers.put(cacheKey, new AckMessage(publishMessage, message -> {
            ValidateUtils.isTrue(message.getFixedHeader().getMessageType() == MqttMessageType.PUBACK, "invalid message type");
            future.complete(true);
            session.responseConsumers.remove(cacheKey);
            LOGGER.info("Qos1消息发送成功...");
            consumer.accept(publishMessage.getVariableHeader().getPacketId());
        }));
        session.write(publishMessage, autoFlush);
        //注册重试
        retry(future, session, publishMessage);
    }

    void publishQos2(AbstractSession session, MqttPublishMessage publishMessage, Consumer<Integer> consumer, boolean autoFlush) {
        MqttQoS qos = publishMessage.getFixedHeader().getQosLevel();
        ValidateUtils.notNull(qos == MqttQoS.EXACTLY_ONCE, "qos is null");
        Integer cacheKey = publishMessage.getVariableHeader().getPacketId();
        CompletableFuture<Boolean> publishFuture = new CompletableFuture<>();
        //只有一次
        session.responseConsumers.put(cacheKey, new AckMessage(publishMessage, message -> {
            ValidateUtils.isTrue(message.getFixedHeader().getMessageType() == MqttMessageType.PUBREC, "invalid message type");
            ValidateUtils.isTrue(Objects.equals(message.getVariableHeader().getPacketId(), publishMessage.getVariableHeader().getPacketId()), "invalid packetId");
            publishFuture.complete(true);
            MqttPubQosVariableHeader variableHeader=new MqttPubQosVariableHeader(message.getVariableHeader().getPacketId());
            //todo
            if(message.getVersion()== MqttVersion.MQTT_5){
                variableHeader.setProperties(new ReasonProperties());
            }
            MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(variableHeader);
            CompletableFuture<Boolean> pubRelFuture = new CompletableFuture<>();
            session.responseConsumers.put(cacheKey, new AckMessage(pubRelMessage, compMessage -> {
                ValidateUtils.isTrue(compMessage.getFixedHeader().getMessageType() == MqttMessageType.PUBCOMP, "invalid message type");
                ValidateUtils.isTrue(Objects.equals(compMessage.getVariableHeader().getPacketId(), pubRelMessage.getVariableHeader().getPacketId()), "invalid packetId");
                pubRelFuture.complete(true);
                LOGGER.info("Qos2消息发送成功...");
                consumer.accept(compMessage.getVariableHeader().getPacketId());
            }));
            //注册重试
            retry(pubRelFuture, session, pubRelMessage);
            session.write(pubRelMessage);
        }));
        session.write(publishMessage, false);
        retry(publishFuture, session, publishMessage);
    }

    protected abstract void retry(CompletableFuture<Boolean> future, AbstractSession session, MqttMessage mqttMessage);
}