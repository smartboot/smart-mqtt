package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttVersion;
import org.smartboot.socket.mqtt.exception.MqttIdentifierRejectedException;
import org.smartboot.socket.transport.WriteBuffer;
import org.smartboot.socket.util.BufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.smartboot.socket.mqtt.message.MqttCodecUtil.isValidClientId;

/**
 * 连接服务端，客户端到服务端的网络连接建立后，客户端发送给服务端的第一个报文必须是 CONNECT 报文。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnectMessage extends MqttMessage {
    /**
     * 可变报头
     */
    private MqttConnectVariableHeader mqttConnectVariableHeader;
    /**
     * 有效载荷
     */
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
        //协议名
        //协议名是表示协议名 MQTT 的 UTF-8 编码的字符串。
        //MQTT 规范的后续版本不会改变这个字符串的偏移和长度。
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文
        final String protocolName = decodeString(buffer);

        //协议级别，8位无符号值
        final byte protocolLevel = buffer.get();

        //连接标志
        final int b1 = BufferUtils.readUnsignedByte(buffer);

        //保持连接
        final int keepAlive = decodeMsbLsb(buffer);

        mqttConnectVariableHeader = new MqttConnectVariableHeader(
                protocolName,
                protocolLevel,
                b1,
                keepAlive);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        //客户端标识符
        // 客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        final String decodedClientId = decodeString(buffer);

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
        byte versionByte = (byte) mqttConnectVariableHeader.getProtocolLevel();
        dos.writeByte(0);
        dos.writeByte((byte) nameBytes.length);
        dos.write(nameBytes);
        dos.writeByte(versionByte);
        byte flag = 0x00;
        if (mqttConnectVariableHeader.hasUserName()) {
            flag = (byte) 0x80;
        }
        if (mqttConnectVariableHeader.hasPassword()) {
            flag |= 0x40;
        }
        if (mqttConnectVariableHeader.isWillFlag()) {
            flag |= 0x04;
            flag |= mqttConnectVariableHeader.willQos() << 3;
            if (mqttConnectVariableHeader.isWillRetain()) {
                flag |= 0x20;
            }
        }
        if (mqttConnectVariableHeader.isCleanSession()) {
            flag |= 0x02;
        }
        dos.writeByte(flag);
        dos.writeShort((short) mqttConnectVariableHeader.keepAliveTimeSeconds());
        //ConnectPayload
        if (mqttConnectPayload.clientIdentifier() != null) {
            dos.writeUTF(mqttConnectPayload.clientIdentifier());
        }
        if (mqttConnectPayload.willTopic() != null) {
            dos.writeUTF(mqttConnectPayload.willTopic());
            dos.writeShort((short) mqttConnectPayload.willMessageInBytes().length);
            dos.write(mqttConnectPayload.willMessageInBytes());
        }
        if (mqttConnectPayload.userName() != null) {
            dos.writeUTF(mqttConnectPayload.userName());
        }
        if (mqttConnectPayload.passwordInBytes() != null) {
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
