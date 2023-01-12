package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends MqttPacketIdentifierMessage<MqttReasonVariableHeader> {
    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public void writeWithoutFixedHeader(MqttWriter mqttWriter) throws IOException {
        MqttReasonVariableHeader variableHeader = getVariableHeader();
        int remainingLength = 2; // variable part only has a message id
        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5) {
            propertiesLength = variableHeader.getProperties().preEncode();
            remainingLength += MqttCodecUtil.getVariableLengthInt(propertiesLength) + propertiesLength;
        }

        MqttCodecUtil.writeVariableLengthInt(mqttWriter, remainingLength);

        MqttCodecUtil.writeMsbLsb(mqttWriter, variableHeader.getPacketId());

        if (version == MqttVersion.MQTT_5) {
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            variableHeader.getProperties().writeTo(mqttWriter);
        }
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer) {

    }

    public MqttUnsubAckMessage(MqttReasonVariableHeader variableHeader) {
        super(MqttFixedHeader.UNSUB_ACK_HEADER, variableHeader);
    }
}
