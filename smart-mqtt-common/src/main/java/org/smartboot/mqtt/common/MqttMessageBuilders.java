package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.payload.MqttSubscribePayload;
import org.smartboot.mqtt.common.message.payload.MqttUnsubscribePayload;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttSubscribeVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

import java.util.ArrayList;
import java.util.List;

public final class MqttMessageBuilders {

    private MqttMessageBuilders() {
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
        private int packetId = -1;
        private PublishProperties publishProperties;

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

        public void packetId(int packetId) {
            this.packetId = packetId;
        }

        public PublishBuilder publishProperties(PublishProperties publishProperties) {
            this.publishProperties = publishProperties;
            return this;
        }

        public MqttPublishMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained);
            if (qos != MqttQoS.AT_LEAST_ONCE && qos != MqttQoS.EXACTLY_ONCE) {
                packetId = -1;
            }
            MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(packetId, topic, publishProperties);
            return new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader, payload);
        }
    }

    public static final class SubscribeBuilder {

        private List<MqttTopicSubscription> subscriptions;
        private int packetId;
        private SubscribeProperties subscribeProperties;

        SubscribeBuilder() {
        }

        public SubscribeBuilder addSubscription(MqttQoS qos, String topic) {
            if (subscriptions == null) {
                subscriptions = new ArrayList<>(5);
            }
            MqttTopicSubscription subscription = new MqttTopicSubscription();
            subscription.setQualityOfService(qos);
            subscription.setTopicFilter(topic);
            subscriptions.add(subscription);
            return this;
        }

        public SubscribeBuilder packetId(int packetId) {
            this.packetId = packetId;
            return this;
        }

        public SubscribeBuilder subscribeProperties(SubscribeProperties subscribeProperties) {
            this.subscribeProperties = subscribeProperties;
            return this;
        }

        public MqttSubscribeMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false);
            MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload();
            mqttSubscribePayload.setTopicSubscriptions(subscriptions);
            MqttSubscribeVariableHeader variableHeader = new MqttSubscribeVariableHeader(packetId, subscribeProperties);
            return new MqttSubscribeMessage(mqttFixedHeader, variableHeader, mqttSubscribePayload);
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

        public MqttUnsubscribeMessage build(ReasonProperties properties) {
            MqttUnsubscribePayload mqttSubscribePayload = new MqttUnsubscribePayload(topicFilters);
            MqttReasonVariableHeader variableHeader = new MqttPubQosVariableHeader(packetId, properties);
            return new MqttUnsubscribeMessage(MqttFixedHeader.UNSUBSCRIBE_HEADER, variableHeader, mqttSubscribePayload);
        }
    }


}
