package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnAckVariableHeader;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttSubscribePayload;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribePayload;

import java.util.ArrayList;
import java.util.List;

public final class MqttMessageBuilders {

    private MqttMessageBuilders() {
    }

    public static ConnAckBuilder connAck() {
        return new ConnAckBuilder();
    }

    public static PingReqBuilder pingReq() {
        return new PingReqBuilder();
    }

    public static PublishBuilder publish() {
        return new PublishBuilder();
    }

    public static SubscribeBuilder subscribe() {
        return new SubscribeBuilder();
    }

    public static UnsubscribeBuilder unsubscribe() {
        return new UnsubscribeBuilder();
    }

    public static final class PublishBuilder {
        private String topic;
        private boolean retained;
        private MqttQoS qos;
        private byte[] payload;
        private int packetId;

        PublishBuilder() {
        }

        public PublishBuilder topicName(String topic) {
            this.topic = topic;
            return this;
        }

        public PublishBuilder retained(boolean retained) {
            this.retained = retained;
            return this;
        }

        public PublishBuilder qos(MqttQoS qos) {
            this.qos = qos;
            return this;
        }

        public PublishBuilder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public PublishBuilder packetId(int packetId) {
            this.packetId = packetId;
            return this;
        }

        public MqttPublishMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
            MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(topic, packetId);
            return new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader, payload);
        }
    }

    public static final class SubscribeBuilder {

        private List<MqttTopicSubscription> subscriptions;
        private int packetId;

        SubscribeBuilder() {
        }

        public SubscribeBuilder addSubscription(MqttQoS qos, String topic) {
            if (subscriptions == null) {
                subscriptions = new ArrayList<>(5);
            }
            subscriptions.add(new MqttTopicSubscription(topic, qos));
            return this;
        }

        public SubscribeBuilder packetId(int packetId) {
            this.packetId = packetId;
            return this;
        }

        public MqttSubscribeMessage build() {
            MqttFixedHeader mqttFixedHeader =
                    new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(subscriptions);
            return new MqttSubscribeMessage(mqttFixedHeader, packetId, mqttSubscribePayload);
        }
    }

    public static final class UnsubscribeBuilder {

        private List<String> topicFilters;
        private int packetId;

        UnsubscribeBuilder() {
        }

        public UnsubscribeBuilder addTopicFilter(String topic) {
            if (topicFilters == null) {
                topicFilters = new ArrayList<String>(5);
            }
            topicFilters.add(topic);
            return this;
        }

        public UnsubscribeBuilder packetId(int packetId) {
            this.packetId = packetId;
            return this;
        }

        public MqttUnsubscribeMessage build() {
            MqttUnsubscribePayload mqttSubscribePayload = new MqttUnsubscribePayload(topicFilters);
            return new MqttUnsubscribeMessage(MqttFixedHeader.UNSUBSCRIBE_HEADER, packetId, mqttSubscribePayload);
        }
    }

    public static final class ConnAckBuilder {

        private MqttConnectReturnCode returnCode;
        private boolean sessionPresent;

        ConnAckBuilder() {
        }

        public ConnAckBuilder returnCode(MqttConnectReturnCode returnCode) {
            this.returnCode = returnCode;
            return this;
        }

        public ConnAckBuilder sessionPresent(boolean sessionPresent) {
            this.sessionPresent = sessionPresent;
            return this;
        }

        public MqttConnAckMessage build() {
            MqttConnAckVariableHeader mqttConnAckVariableHeader =
                    new MqttConnAckVariableHeader(returnCode, sessionPresent);
            return new MqttConnAckMessage(mqttConnAckVariableHeader);
        }
    }

    public static final class PingReqBuilder {

        private MqttConnectReturnCode returnCode;
        private boolean sessionPresent;

        PingReqBuilder() {
        }

        public MqttPingReqMessage build() {
            return new MqttPingReqMessage();
        }
    }
}
