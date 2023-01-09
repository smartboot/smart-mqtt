package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.ConnectAckProperties;
import org.smartboot.mqtt.common.message.properties.MqttProperties;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.*;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttVariableMessage<MqttConnAckVariableHeader> {
    private static final int PROPERTIES_BITS = SESSION_EXPIRY_INTERVAL_BIT | RECEIVE_MAXIMUM_BIT | MAXIMUM_QOS_BIT | RETAIN_AVAILABLE_BIT | MAXIMUM_PACKET_SIZE_BIT | ASSIGNED_CLIENT_IDENTIFIER_BIT | TOPIC_ALIAS_MAXIMUM_BIT | REASON_STRING_BIT | USER_PROPERTY_BIT | WILDCARD_SUBSCRIPTION_AVAILABLE_BIT | SUBSCRIPTION_IDENTIFIER_AVAILABLE_BIT | SHARED_SUBSCRIPTION_AVAILABLE_BIT | SERVER_KEEP_ALIVE_BIT | RESPONSE_INFORMATION_BIT | SERVER_REFERENCE_BIT | AUTHENTICATION_METHOD_BIT | AUTHENTICATION_DATA_BIT;

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

        //MQTT 5.0规范
        ConnectAckProperties properties = null;
        if (version == MqttVersion.MQTT_5) {
            MqttProperties mqttProperties = new MqttProperties();
            mqttProperties.decode(buffer, PROPERTIES_BITS);
            properties = new ConnectAckProperties(mqttProperties);
        }
        setVariableHeader(new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent, properties));
    }


    @Override
    public void writeTo(MqttWriter mqttWriter) {
        MqttConnAckVariableHeader variableHeader = getVariableHeader();
        mqttWriter.writeByte(getFixedHeaderByte(fixedHeader));
        int remaining = 2;
        if (version == MqttVersion.MQTT_5) {
            int propertiesLength = preEncodeProperties(variableHeader.getProperties());
            remaining += propertiesLength + MqttCodecUtil.getVariableLengthInt(propertiesLength);
            //用变长字节整数来编码，表示可变报头的长度。
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, remaining);

            mqttWriter.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
            mqttWriter.writeByte(variableHeader.connectReturnCode().getCode());

            //CONNACK报文可变报头中的属性长度，编码为变长字节整数。
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            writeProperties(mqttWriter, variableHeader.getProperties());
        } else {
            //表示可变报头的长度。对于 CONNACK 报文这个值等于 2。
            mqttWriter.writeByte((byte) 2);
            mqttWriter.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
            mqttWriter.writeByte(variableHeader.connectReturnCode().getCode());
        }
    }

    /**
     * 预先编码属性
     *
     * @return 属性长度
     */
    private int preEncodeProperties(ConnectAckProperties properties) {
        return 0;
    }

    private void writeProperties(MqttWriter buffer, ConnectAckProperties properties) {

    }
}
