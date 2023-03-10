package org.smartboot.mqtt.common.protocol;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;

final class MqttMessageFactory {

    private MqttMessageFactory() {
    }

    public static MqttFixedHeader newMqttFixedHeader(MqttMessageType messageType, boolean dup, int qosLevel, boolean retain) {
        switch (messageType) {
            case CONNECT:
                return MqttFixedHeader.CONNECT_HEADER;
            case CONNACK:
                return MqttFixedHeader.CONN_ACK_HEADER;
            case SUBSCRIBE:
                return MqttFixedHeader.SUBSCRIBE_HEADER;
            case SUBACK:
                return MqttFixedHeader.SUB_ACK_HEADER;
            case UNSUBACK:
                return MqttFixedHeader.UNSUB_ACK_HEADER;
            case UNSUBSCRIBE:
                return MqttFixedHeader.UNSUBSCRIBE_HEADER;
            case PUBLISH:
                if (dup || retain) {
                    return new MqttFixedHeader(messageType, dup, MqttQoS.valueOf(qosLevel), retain);
                }
                switch (qosLevel) {
                    case 0:
                        return MqttFixedHeader.PUB_QOS0_HEADER;
                    case 1:
                        return MqttFixedHeader.PUB_QOS1_HEADER;
                    case 2:
                        return MqttFixedHeader.PUB_QOS2_HEADER;
                    default:
                        return MqttFixedHeader.PUB_FAILURE_HEADER;
                }
            case PUBACK:
                return MqttFixedHeader.PUB_ACK_HEADER;
            case PUBREC:
                return MqttFixedHeader.PUB_REC_HEADER;
            case PUBREL:
                return MqttFixedHeader.PUB_REL_HEADER;
            case PUBCOMP:
                return MqttFixedHeader.PUB_COMP_HEADER;
            case PINGREQ:
                return MqttFixedHeader.PING_REQ_HEADER;
            case PINGRESP:
                return MqttFixedHeader.PING_RESP_HEADER;
            case DISCONNECT:
                return MqttFixedHeader.DISCONNECT_HEADER;
            default:
                throw new IllegalArgumentException("unknown message type: " + messageType);
        }
    }

    public static MqttMessage newMessage(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.getMessageType()) {
            case CONNECT:
                return new MqttConnectMessage(mqttFixedHeader);

            case CONNACK:
                return new MqttConnAckMessage(mqttFixedHeader);

            case SUBSCRIBE:
                return new MqttSubscribeMessage(mqttFixedHeader);

            case SUBACK:
                return new MqttSubAckMessage(mqttFixedHeader);

            case UNSUBACK:
                return new MqttUnsubAckMessage(mqttFixedHeader);

            case UNSUBSCRIBE:
                return new MqttUnsubscribeMessage(mqttFixedHeader);

            case PUBLISH:
                return new MqttPublishMessage(mqttFixedHeader);

            case PUBACK:
                return new MqttPubAckMessage(mqttFixedHeader);
            case PUBREC:
                return new MqttPubRecMessage(mqttFixedHeader);
            case PUBREL:
                return new MqttPubRelMessage(mqttFixedHeader);
            case PUBCOMP:
                return new MqttPubCompMessage(mqttFixedHeader);

            case PINGREQ:
                return new MqttPingReqMessage(mqttFixedHeader);
            case PINGRESP:
                return new MqttPingRespMessage(mqttFixedHeader);
            case DISCONNECT:
                return new MqttDisconnectMessage(mqttFixedHeader);

            default:
                throw new IllegalArgumentException("unknown message type: " + mqttFixedHeader.getMessageType());
        }
    }

}
