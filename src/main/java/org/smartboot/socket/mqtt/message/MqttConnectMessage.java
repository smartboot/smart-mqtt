package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttVersion;
import org.smartboot.socket.mqtt.exception.MqttIdentifierRejectedException;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.smartboot.socket.mqtt.message.MqttCodecUtil.isValidClientId;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnectMessage extends MqttMessage {
    private MqttConnectVariableHeader mqttConnectVariableHeader;
    private MqttConnectPayload mqttConnectPayload;

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader, MqttConnectVariableHeader mqttConnectVariableHeader, MqttConnectPayload mqttConnectPayload) {
        super(mqttFixedHeader);
        this.mqttConnectVariableHeader = mqttConnectVariableHeader;
        this.mqttConnectPayload = mqttConnectPayload;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final String protoString = decodeString(buffer);

        final byte protocolLevel = buffer.get();

        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(protoString, protocolLevel);

        final int b1 = BufferUtils.readUnsignedByte(buffer);

        final int keepAlive = decodeMsbLsb(buffer);

        final boolean hasUserName = (b1 & 0x80) == 0x80;
        final boolean hasPassword = (b1 & 0x40) == 0x40;
        final boolean willRetain = (b1 & 0x20) == 0x20;
        final int willQos = (b1 & 0x18) >> 3;
        final boolean willFlag = (b1 & 0x04) == 0x04;
        final boolean cleanSession = (b1 & 0x02) == 0x02;
        if (mqttVersion == MqttVersion.MQTT_3_1_1) {
            final boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
            if (!zeroReservedFlag) {
                // MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
                // set to zero and disconnect the Client if it is not zero.
                // See http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
                throw new DecoderException("non-zero reserved flag");
            }
        }

        mqttConnectVariableHeader = new MqttConnectVariableHeader(
                mqttVersion.protocolName(),
                mqttVersion.protocolLevel(),
                hasUserName,
                hasPassword,
                willRetain,
                willQos,
                willFlag,
                cleanSession,
                keepAlive);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final String decodedClientId = decodeString(buffer);
        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(mqttConnectVariableHeader.name(),
                (byte) mqttConnectVariableHeader.version());
        if (!isValidClientId(mqttVersion, decodedClientId)) {
            throw new MqttIdentifierRejectedException("invalid clientIdentifier: " + decodedClientId);
        }

        String decodedWillTopic = null;
        byte[] decodedWillMessage = null;
        if (mqttConnectVariableHeader.isWillFlag()) {
            decodedWillTopic = decodeString(buffer, 0, 32767);
            decodedWillMessage = decodeByteArray(buffer);
        }
        String decodedUserName = null;
        byte[] decodedPassword = null;
        if (mqttConnectVariableHeader.hasUserName()) {
            decodedUserName = decodeString(buffer);
        }
        if (mqttConnectVariableHeader.hasPassword()) {
            decodedPassword = decodeByteArray(buffer);
        }

        mqttConnectPayload = new MqttConnectPayload(decodedClientId, decodedWillTopic, decodedWillMessage, decodedUserName, decodedPassword);
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        //VariableHeader
        byte[] nameBytes = mqttConnectVariableHeader.name().getBytes(StandardCharsets.UTF_8);
        byte versionByte = (byte) mqttConnectVariableHeader.version();
        dos.writeByte(0);
        dos.writeByte((byte) nameBytes.length);
        dos.write(nameBytes);
        dos.writeByte(versionByte);
        byte flag = 0x00;
        if (mqttConnectVariableHeader.hasUserName()){
            flag = (byte) 0x80;
        }
        if (mqttConnectVariableHeader.hasPassword()){
            flag |= 0x40;
        }
        if (mqttConnectVariableHeader.isWillFlag()){
            flag |= 0x04;
            flag |= mqttConnectVariableHeader.willQos() << 3;
            if (mqttConnectVariableHeader.isWillRetain()){
                flag |= 0x20;
            }
        }
        if (mqttConnectVariableHeader.isCleanSession()){
            flag |= 0x02;
        }
        dos.writeByte(flag);
        dos.writeShort((short) mqttConnectVariableHeader.keepAliveTimeSeconds());
        //ConnectPayload
        if (mqttConnectPayload.clientIdentifier()!=null){
            dos.writeUTF(mqttConnectPayload.clientIdentifier());
        }
        if (mqttConnectPayload.willTopic() != null){
            dos.writeUTF(mqttConnectPayload.willTopic());
            dos.writeShort((short) mqttConnectPayload.willMessageInBytes().length);
            dos.write(mqttConnectPayload.willMessageInBytes());
        }
        if (mqttConnectPayload.userName() != null){
            dos.writeUTF(mqttConnectPayload.userName());
        }
        if (mqttConnectPayload.passwordInBytes() != null){
            dos.writeShort((short) mqttConnectPayload.passwordInBytes().length);
            dos.write(mqttConnectPayload.passwordInBytes());
        }
        dos.flush();
        byte[] varAndPayloadBytes = baos.toByteArray();
        baos.reset();
        dos.writeByte(getFixedHeaderByte1(mqttFixedHeader));
        //todo 多个字节长度失效
        dos.write(encodeMBI(varAndPayloadBytes.length));
        dos.write(varAndPayloadBytes);
        dos.flush();
        byte[] data = baos.toByteArray();
        writeBuffer.writeAndFlush(data);
    }

    public MqttConnectPayload getPayload() {
        return mqttConnectPayload;
    }

    public MqttConnectVariableHeader getVariableHeader() {
        return mqttConnectVariableHeader;
    }
}
