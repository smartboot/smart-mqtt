/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.util;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPacketIdentifierMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttTopicSubscription;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.payload.MqttSubscribePayload;
import org.smartboot.mqtt.common.message.payload.MqttUnsubscribePayload;
import org.smartboot.mqtt.common.message.variable.MqttPacketIdVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttSubscribeVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;
import org.smartboot.mqtt.common.message.variable.properties.SubscribeProperties;

import java.util.ArrayList;
import java.util.List;

public final class MqttMessageBuilders {

    public interface MessageBuilder<T extends MqttPacketIdentifierMessage<? extends MqttPacketIdVariableHeader>> {
        MessageBuilder packetId(int packetId);

        MqttQoS qos();

        T build();
    }

    private MqttMessageBuilders() {
    }

    public static SubscribeBuilder subscribe() {
        return new SubscribeBuilder();
    }

    public static UnsubscribeBuilder unsubscribe() {
        return new UnsubscribeBuilder();
    }


    public static final class SubscribeBuilder implements MessageBuilder<MqttSubscribeMessage> {

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

        @Override
        public MqttQoS qos() {
            return MqttQoS.AT_LEAST_ONCE;
        }

        public SubscribeBuilder subscribeProperties(SubscribeProperties subscribeProperties) {
            this.subscribeProperties = subscribeProperties;
            return this;
        }

        public MqttSubscribeMessage build() {
            MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload();
            mqttSubscribePayload.setTopicSubscriptions(subscriptions);
            MqttSubscribeVariableHeader variableHeader = new MqttSubscribeVariableHeader(packetId, subscribeProperties);
            return new MqttSubscribeMessage(MqttFixedHeader.SUBSCRIBE_HEADER, variableHeader, mqttSubscribePayload);
        }
    }

    public static final class UnsubscribeBuilder implements MessageBuilder<MqttUnsubscribeMessage> {

        private List<String> topicFilters;
        private int packetId;
        private ReasonProperties properties;

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

        @Override
        public MqttQoS qos() {
            return MqttQoS.AT_LEAST_ONCE;
        }

        public void properties(ReasonProperties properties) {
            this.properties = properties;
        }

        public MqttUnsubscribeMessage build() {
            MqttUnsubscribePayload mqttSubscribePayload = new MqttUnsubscribePayload(topicFilters);
            MqttReasonVariableHeader variableHeader = new MqttPubQosVariableHeader(packetId, properties);
            return new MqttUnsubscribeMessage(MqttFixedHeader.UNSUBSCRIBE_HEADER, variableHeader, mqttSubscribePayload);
        }
    }


}
