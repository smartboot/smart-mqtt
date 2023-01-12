package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttProtocolEnum;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import org.smartboot.mqtt.common.message.variable.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.message.variable.properties.WillProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 连接服务端，客户端到服务端的网络连接建立后，客户端发送给服务端的第一个报文必须是 CONNECT 报文。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnectMessage extends MqttVariableMessage<MqttConnectVariableHeader> {


    /**
     * 有效载荷
     */
    private MqttConnectPayload mqttConnectPayload;

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnectMessage(MqttConnectVariableHeader mqttConnectVariableHeader, MqttConnectPayload mqttConnectPayload) {
        super(MqttFixedHeader.CONNECT_HEADER);
        setVariableHeader(mqttConnectVariableHeader);
        this.mqttConnectPayload = mqttConnectPayload;
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        //协议名
        //协议名是表示协议名 MQTT 的 UTF-8 编码的字符串。
        //MQTT 规范的后续版本不会改变这个字符串的偏移和长度。
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文
        final String protocolName = MqttCodecUtil.decodeUTF8(buffer);

        //协议级别，8位无符号值
        final byte protocolLevel = buffer.get();

        //连接标志
        final int b1 = BufferUtils.readUnsignedByte(buffer);

        //保持连接
        final int keepAlive = MqttCodecUtil.decodeMsbLsb(buffer);

        version = MqttVersion.getByProtocolWithVersion(MqttProtocolEnum.getByName(protocolName), protocolLevel);

        //MQTT 5.0规范
        ConnectProperties properties = null;
        if (version == MqttVersion.MQTT_5) {
            properties = new ConnectProperties();
            properties.decode(buffer);
        }

        setVariableHeader(new MqttConnectVariableHeader(protocolName, protocolLevel, b1, keepAlive, properties));


    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        MqttConnectVariableHeader variableHeader = getVariableHeader();
        //客户端标识符
        // 客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        final String decodedClientId = MqttCodecUtil.decodeUTF8(buffer);

        WillMessage willMessage = null;
        //如果遗嘱标志被设置为 1，有效载荷的下一个字段是遗嘱主题（Will Topic）。
        // 遗嘱主题必须是 1.5.3 节定义的 UTF-8 编码字符串
        if (variableHeader.isWillFlag()) {
            willMessage = new WillMessage();
            // MQTT 5.0 遗嘱属性
            //如果遗嘱标志（Will Flag）被设置为1，有效载荷的下一个字段是遗嘱属性（Will Properties）。
            // 遗嘱属性字段定义了遗嘱消息（Will Message）将何时被发布，以及被发布时的应用消息（Application Message）属性。
            if (version == MqttVersion.MQTT_5) {
                WillProperties willProperties = new WillProperties();
                willProperties.decode(buffer);
                willMessage.setProperties(willProperties);
            }
            willMessage.setWillTopic(MqttCodecUtil.decodeUTF8(buffer, 0, 32767));
            willMessage.setWillMessage(MqttCodecUtil.decodeByteArray(buffer));
        }
        String decodedUserName = null;
        byte[] decodedPassword = null;
        // 如果用户名（User Name）标志被设置为 1，有效载荷的下一个字段就是它。
        // 用户名必须是 1.5.3 节定义的 UTF-8 编码字符串 [MQTT-3.1.3-11]。
        // 服务端可以将它用于身份验证和授权。
        if (variableHeader.hasUserName()) {
            decodedUserName = MqttCodecUtil.decodeUTF8(buffer);
        }
        // 密码字段包含一个两字节的长度字段，
        // 长度表示二进制数据的字节数（不包含长度字段本身占用的两个字节），
        // 后面跟着 0 到 65535 字节的二进制数据。
        if (variableHeader.hasPassword()) {
            decodedPassword = MqttCodecUtil.decodeByteArray(buffer);
        }

        mqttConnectPayload = new MqttConnectPayload(decodedClientId, willMessage, decodedUserName, decodedPassword);
    }


    @Override
    public void writeWithoutFixedHeader(MqttWriter mqttWriter) throws IOException {
        MqttConnectVariableHeader variableHeader = getVariableHeader();
        //VariableHeader
        byte[] clientIdBytes = MqttCodecUtil.encodeUTF8(mqttConnectPayload.clientIdentifier());
        //剩余长度等于可变报头的长度（10 字节）加上有效载荷的长度。
        int remainingLength = 10 + clientIdBytes.length;

        //遗嘱
        if (variableHeader.isWillFlag()) {
            remainingLength += mqttConnectPayload.getWillMessage().preEncode();
        }

        //用户名
        byte[] userNameBytes = null;
        if (mqttConnectPayload.userName() != null) {
            userNameBytes = MqttCodecUtil.encodeUTF8(mqttConnectPayload.userName());
            remainingLength += userNameBytes.length;
        }
        //密码
        if (mqttConnectPayload.passwordInBytes() != null) {
            remainingLength += 2 + mqttConnectPayload.passwordInBytes().length;
        }

        int propertiesLength = 0;
        if (version == MqttVersion.MQTT_5) {
            ConnectProperties properties = variableHeader.getProperties();
            propertiesLength = properties.preEncode();
            remainingLength += MqttCodecUtil.getVariableLengthInt(propertiesLength) + propertiesLength;
        }


        //第一部分：固定报头
        MqttCodecUtil.writeVariableLengthInt(mqttWriter, remainingLength);


        //第二部分：可变报头，10字节
        //协议名
        byte[] nameBytes = variableHeader.protocolName().getBytes(StandardCharsets.UTF_8);
        ValidateUtils.isTrue(nameBytes.length == 4, "invalid protocol name");
        byte versionByte = variableHeader.getProtocolLevel();
        mqttWriter.writeShort((short) nameBytes.length);
        mqttWriter.write(nameBytes);
        //协议级别
        mqttWriter.writeByte(versionByte);
        //连接标志
        byte connectFlag = 0x00;
        if (variableHeader.hasUserName()) {
            connectFlag = (byte) 0x80;
        }
        if (variableHeader.hasPassword()) {
            connectFlag |= 0x40;
        }
        if (variableHeader.isWillFlag()) {
            connectFlag |= 0x04;
            connectFlag |= variableHeader.willQos() << 3;
            if (variableHeader.isWillRetain()) {
                connectFlag |= 0x20;
            }
        }
        if (variableHeader.isCleanSession()) {
            connectFlag |= 0x02;
        }
        mqttWriter.writeByte(connectFlag);
        //保持连接
        mqttWriter.writeShort((short) variableHeader.keepAliveTimeSeconds());

        // Connect属性
        if (version == MqttVersion.MQTT_5) {
            MqttCodecUtil.writeVariableLengthInt(mqttWriter, propertiesLength);
            ConnectProperties properties = variableHeader.getProperties();
            properties.writeTo(mqttWriter);
        }

        //第三部分：有效载荷
        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        mqttWriter.write(clientIdBytes);

        //遗嘱
        if (variableHeader.isWillFlag()) {
            mqttConnectPayload.getWillMessage().writeTo(mqttWriter);
        }
        //用户名
        if (userNameBytes != null) {
            mqttWriter.write(userNameBytes);
        }
        //密码
        if (mqttConnectPayload.passwordInBytes() != null) {
            mqttWriter.writeShort((short) mqttConnectPayload.passwordInBytes().length);
            mqttWriter.write(mqttConnectPayload.passwordInBytes());
        }
    }

    public MqttConnectPayload getPayload() {
        return mqttConnectPayload;
    }

}
