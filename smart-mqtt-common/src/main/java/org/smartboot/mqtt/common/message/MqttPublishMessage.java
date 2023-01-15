package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttPublishPayload;
import org.smartboot.mqtt.common.message.variable.MqttPublishVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttVariableMessage<MqttPublishVariableHeader> {
    private static final MqttPublishPayload EMPTY_BYTES = new MqttPublishPayload(new byte[0]);
    private MqttPublishPayload payload;

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader, byte[] payload) {
        super(mqttFixedHeader);
        setVariableHeader(mqttPublishVariableHeader);
        this.payload = new MqttPublishPayload(payload);
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        final String decodedTopic = MqttCodecUtil.decodeUTF8(buffer);
        //PUBLISH 报文中的主题名不能包含通配符
        if (MqttUtil.containsTopicWildcards(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int packetId = -1;
        //只有当 QoS 等级是 1 或 2 时，报文标识符（Packet Identifier）字段才能出现在 PUBLISH 报文中。
        if (fixedHeader.getQosLevel().value() > 0) {
            packetId = decodeMessageId(buffer);
        }
        MqttPublishVariableHeader variableHeader;
        if (version == MqttVersion.MQTT_5) {
            PublishProperties properties = new PublishProperties();
            properties.decode(buffer);
            variableHeader = new MqttPublishVariableHeader(packetId, decodedTopic, properties);
        } else {
            variableHeader = new MqttPublishVariableHeader(packetId, decodedTopic, null);
        }

        setVariableHeader(variableHeader);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        int readLength = fixedHeader.remainingLength() - getVariableHeaderLength();
        if (readLength == 0) {
            payload = EMPTY_BYTES;
        } else {
            byte[] bytes = new byte[readLength];
            buffer.get(bytes);
            payload = new MqttPublishPayload(bytes);
        }
    }


    @Override
    public MqttPublishPayload getPayload() {
        return payload;
    }
}
