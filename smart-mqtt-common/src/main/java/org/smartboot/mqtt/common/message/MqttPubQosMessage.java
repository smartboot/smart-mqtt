package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.ReasonProperties;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubQosMessage extends MqttPacketIdentifierMessage<MqttPubQosVariableHeader> {

    public MqttPubQosMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubQosMessage(MqttFixedHeader pubRecHeader, MqttPubQosVariableHeader variableHeader) {
        super(pubRecHeader);
        this.variableHeader = variableHeader;
    }

    @Override
    protected final void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        MqttPubQosVariableHeader header = new MqttPubQosVariableHeader(packetId);
        if (version == MqttVersion.MQTT_5) {
            //如果剩余长度为2，则表示使用原因码0x00 （成功）
            if (fixedHeader.remainingLength() == 2) {
                header.setReasonCode((byte) 0x00);
            } else if (fixedHeader.remainingLength() < 4) {
                //如果剩余长度小于4，则表示没有属性长度字段。
                header.setReasonCode(buffer.get());
            } else {
                header.setReasonCode(buffer.get());
                ReasonProperties properties = new ReasonProperties();
                properties.decode(buffer);
                header.setProperties(properties);
            }

        }
        setVariableHeader(header);
    }

    @Override
    public final void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttPubQosVariableHeader variableHeader = getVariableHeader();
        int remainingLength = 2; // variable part only has a message id
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));

        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5 && variableHeader.getReasonCode() != 0) {
            remainingLength += 1;
            propertiesLength = variableHeader.getProperties().preEncode();
            remainingLength += MqttCodecUtil.getVariableLengthInt(propertiesLength) + propertiesLength;
        }

        MqttCodecUtil.writeVariableLengthInt(mqttWriter, remainingLength);

        mqttWriter.writeShort((short) variableHeader.getPacketId());

        if (version == MqttVersion.MQTT_5 && variableHeader.getReasonCode() != 0) {
            mqttWriter.writeByte(variableHeader.getReasonCode());
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            variableHeader.getProperties().writeTo(mqttWriter);
        }
    }

}
