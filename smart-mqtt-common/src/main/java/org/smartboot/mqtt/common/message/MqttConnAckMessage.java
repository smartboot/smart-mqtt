package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.ConnectAckProperties;
import org.smartboot.mqtt.common.message.variable.MqttConnAckVariableHeader;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttVariableMessage<MqttConnAckVariableHeader> {


    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(MqttFixedHeader.CONN_ACK_HEADER);
        setVariableHeader(mqttConnAckVariableHeader);
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();

        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
        //MQTT 5.0规范
        if (version == MqttVersion.MQTT_5) {
            ConnectAckProperties properties = new ConnectAckProperties();
            properties.decode(buffer);
            variableHeader.setProperties(properties);
        }
        setVariableHeader(variableHeader);
    }


    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttConnAckVariableHeader variableHeader = getVariableHeader();
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));
        int remaining = 2;
        if (version == MqttVersion.MQTT_5) {
            int propertiesLength = variableHeader.getProperties().preEncode();
            remaining += propertiesLength + MqttCodecUtil.getVariableLengthInt(propertiesLength);
            //用变长字节整数来编码，表示可变报头的长度。
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, remaining);

            mqttWriter.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
            mqttWriter.writeByte(variableHeader.connectReturnCode().getCode());

            //CONNACK报文可变报头中的属性长度，编码为变长字节整数。
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            variableHeader.getProperties().writeTo(mqttWriter);
        } else {
            //表示可变报头的长度。对于 CONNACK 报文这个值等于 2。
            mqttWriter.writeByte((byte) 2);
            mqttWriter.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
            mqttWriter.writeByte(variableHeader.connectReturnCode().getCode());
        }
    }
}
