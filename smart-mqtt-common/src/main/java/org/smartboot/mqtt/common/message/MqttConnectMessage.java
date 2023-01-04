package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttProtocolEnum;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.ConnectProperties;
import org.smartboot.mqtt.common.message.properties.UserProperty;
import org.smartboot.mqtt.common.message.properties.WillProperties;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;
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
    /**
     * CONNECT属性
     */
    private ConnectProperties connectProperties;

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnectMessage(MqttConnectVariableHeader mqttConnectVariableHeader, MqttConnectPayload mqttConnectPayload) {
        super(MqttFixedHeader.CONNECT_HEADER);
        setVariableHeader(mqttConnectVariableHeader);
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

        version = MqttVersion.getByProtocolWithVersion(MqttProtocolEnum.getByName(protocolName), protocolLevel);

        //MQTT 5.0规范
        if (version == MqttVersion.MQTT_5) {
            decodeConnectProperties(buffer);
        }

        setVariableHeader(new MqttConnectVariableHeader(protocolName, protocolLevel, b1, keepAlive));


    }

    private void decodeConnectProperties(ByteBuffer buffer) {
        int remainingLength = decodeVariableByteInteger(buffer);
        connectProperties = new ConnectProperties();
        if (remainingLength == 0) {
            return;
        }

        int sessionExpiryInterval = -1;
        int topicAliasMaximum = -1;
        byte requestResponseInformation = -1;
        byte requestProblemInformation = -1;
        int position;
        while (remainingLength > 0) {
            position = buffer.position();
            switch (buffer.get()) {
                //会话过期间隔
                case MqttPropertyConstant.SESSION_EXPIRY_INTERVAL:
                    //包含多个会话过期间隔（Session Expiry Interval）将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(sessionExpiryInterval == -1, "");
                    sessionExpiryInterval = buffer.getInt();
                    connectProperties.setSessionExpiryInterval(sessionExpiryInterval);
                    break;
                //接收最大值
                case MqttPropertyConstant.RECEIVE_MAXIMUM:
                    connectProperties.setReceiveMaximum(decodeMsbLsb(buffer));
                    break;
                //最大报文长度
                case MqttPropertyConstant.MAXIMUM_PACKET_SIZE:
                    //包含多个最大报文长度（Maximum Packet Size）或者最大报文长度（Maximum Packet Size）值为0将造成协议错误。
                    ValidateUtils.isTrue(connectProperties.getMaximumPacketSize() == null, "");
                    int maximumPacketSize = buffer.getInt();
                    ValidateUtils.isTrue(maximumPacketSize > 0, "");
                    connectProperties.setMaximumPacketSize(buffer.getInt());
                    break;
                //主题别名最大值
                case MqttPropertyConstant.TOPIC_ALIAS_MAXIMUM:
                    //跟随其后的是用双字节整数表示的主题别名最大值（Topic Alias Maximum）。
                    // 包含多个主题别名最大值（Topic Alias Maximum）将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(topicAliasMaximum == -1, "");
                    topicAliasMaximum = decodeMsbLsb(buffer);
                    connectProperties.setTopicAliasMaximum(decodeMsbLsb(buffer));
                    ValidateUtils.isTrue(topicAliasMaximum >= 0, "");
                    break;
                //请求响应信息
                case MqttPropertyConstant.REQUEST_RESPONSE_INFORMATION:
                    ValidateUtils.isTrue(requestResponseInformation == -1, "");
                    requestResponseInformation = buffer.get();
                    connectProperties.setRequestResponseInformation(requestResponseInformation);
                    ValidateUtils.isTrue(requestResponseInformation == 0 || requestResponseInformation == 1, "");
                    break;
                //请求问题信息
                case MqttPropertyConstant.REQUEST_PROBLEM_INFORMATION:
                    ValidateUtils.isTrue(requestProblemInformation == -1, "");
                    requestProblemInformation = buffer.get();
                    connectProperties.setRequestProblemInformation(requestProblemInformation);
                    ValidateUtils.isTrue(requestProblemInformation == 0 || requestProblemInformation == 1, "");
                    break;
                //用户属性
                case MqttPropertyConstant.USER_PROPERTY:
                    String key = decodeString(buffer);
                    String value = decodeString(buffer);
                    connectProperties.getUserProperties().add(new UserProperty(key, value));
                    break;
                //认证方法
                case MqttPropertyConstant.AUTHENTICATION_METHOD:
                    //包含多个认证方法将造成协议错误
                    ValidateUtils.isTrue(connectProperties.getAuthenticationMethod() == null, "");
                    connectProperties.setAuthenticationMethod(decodeString(buffer));
                    break;
                //认证数据
                case MqttPropertyConstant.AUTHENTICATION_DATA:
                    //包含多个认证数据（Authentication Data）将造成协议错误
                    ValidateUtils.isTrue(connectProperties.getAuthenticationData() == null, "");
                    byte[] data = decodeByteArray(buffer);
                    connectProperties.setAuthenticationData(data);
                    break;
            }
            remainingLength -= buffer.position() - position;
        }
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        MqttConnectVariableHeader variableHeader = getVariableHeader();
        //客户端标识符
        // 客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        final String decodedClientId = decodeString(buffer);

        // MQTT 5.0 遗嘱属性
        WillProperties willProperties = null;
        if (version == MqttVersion.MQTT_5) {
            willProperties = new WillProperties();
            int remainingLength = decodeVariableByteInteger(buffer);
            int willDelayInterval = -1;
            int messageExpiryInterval = -1;
            byte payloadFormatIndicator = -1;
            int position;
            while (remainingLength > 0) {
                position = buffer.position();
                switch (buffer.get()) {
                    case MqttPropertyConstant.WILL_DELAY_INTERVAL:
                        //包含多个遗嘱延时间隔将造成协议错误（Protocol Error）
                        ValidateUtils.isTrue(willDelayInterval == -1, "");
                        willDelayInterval = buffer.getInt();
                        willProperties.setWillDelayInterval(willDelayInterval);
                        ValidateUtils.isTrue(willDelayInterval >= 0, "");
                        break;
                    //载荷格式指示
                    case MqttPropertyConstant.PAYLOAD_FORMAT_INDICATOR:
                        //包含多个载荷格式指示（Payload Format Indicator）将造成协议错误（Protocol Error）
                        ValidateUtils.isTrue(payloadFormatIndicator == -1, "");
                        payloadFormatIndicator = buffer.get();
                        willProperties.setPayloadFormatIndicator(payloadFormatIndicator);
                        ValidateUtils.isTrue(payloadFormatIndicator == 0 || payloadFormatIndicator == 1, "");
                        break;
                    //消息过期间隔
                    case MqttPropertyConstant.MESSAGE_EXPIRY_INTERVAL:
                        //包含多个消息过期间隔将导致协议错误（Protocol Error）
                        ValidateUtils.isTrue(messageExpiryInterval == -1, "");
                        messageExpiryInterval = buffer.getInt();
                        willProperties.setMessageExpiryInterval(messageExpiryInterval);
                        ValidateUtils.isTrue(messageExpiryInterval > 0, "");
                        break;
                    //内容类型
                    case MqttPropertyConstant.CONTENT_TYPE:
                        ValidateUtils.isTrue(willProperties.getContentType() == null, "");
                        willProperties.setContentType(decodeString(buffer));
                        break;
                    //响应主题
                    case MqttPropertyConstant.RESPONSE_TOPIC:
                        ValidateUtils.isTrue(willProperties.getResponseTopic() == null, "");
                        willProperties.setResponseTopic(decodeString(buffer));
                        break;
                    //对比数据
                    case MqttPropertyConstant.CORRELATION_DATA:
                        ValidateUtils.isTrue(willProperties.getCorrelationData() == null, "");
                        willProperties.setCorrelationData(decodeByteArray(buffer));
                        break;
                    //用户属性
                    case MqttPropertyConstant.USER_PROPERTY:
                        String key = decodeString(buffer);
                        String value = decodeString(buffer);
                        willProperties.getUserProperties().add(new UserProperty(key, value));
                        break;
                }
                remainingLength -= buffer.position() - position;
            }
        }

        String decodedWillTopic = null;
        byte[] decodedWillMessage = null;
        //如果遗嘱标志被设置为 1，有效载荷的下一个字段是遗嘱主题（Will Topic）。
        // 遗嘱主题必须是 1.5.3 节定义的 UTF-8 编码字符串
        if (variableHeader.isWillFlag()) {
            decodedWillTopic = decodeString(buffer, 0, 32767);
            decodedWillMessage = decodeByteArray(buffer);
        }
        String decodedUserName = null;
        byte[] decodedPassword = null;
        // 如果用户名（User Name）标志被设置为 1，有效载荷的下一个字段就是它。
        // 用户名必须是 1.5.3 节定义的 UTF-8 编码字符串 [MQTT-3.1.3-11]。
        // 服务端可以将它用于身份验证和授权。
        if (variableHeader.hasUserName()) {
            decodedUserName = decodeString(buffer);
        }
        // 密码字段包含一个两字节的长度字段，
        // 长度表示二进制数据的字节数（不包含长度字段本身占用的两个字节），
        // 后面跟着 0 到 65535 字节的二进制数据。
        if (variableHeader.hasPassword()) {
            decodedPassword = decodeByteArray(buffer);
        }

        mqttConnectPayload = new MqttConnectPayload(decodedClientId, decodedWillTopic, decodedWillMessage, decodedUserName, decodedPassword, willProperties);
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) throws IOException {
        MqttConnectVariableHeader variableHeader = getVariableHeader();
        //VariableHeader
        byte[] clientIdBytes = encodeUTF8(mqttConnectPayload.clientIdentifier());
        //剩余长度等于可变报头的长度（10 字节）加上有效载荷的长度。
        int remainingLength = 10 + clientIdBytes.length;

        //遗嘱
        byte[] willTopicBytes = null;
        if (mqttConnectPayload.willTopic() != null) {
            willTopicBytes = encodeUTF8(mqttConnectPayload.willTopic());
            remainingLength += willTopicBytes.length + 2 + mqttConnectPayload.willMessageInBytes().length;
        }

        //用户名
        byte[] userNameBytes = null;
        if (mqttConnectPayload.userName() != null) {
            userNameBytes = encodeUTF8(mqttConnectPayload.userName());
            remainingLength += userNameBytes.length;
        }
        //密码
        if (mqttConnectPayload.passwordInBytes() != null) {
            remainingLength += 2 + mqttConnectPayload.passwordInBytes().length;
        }


        //第一部分：固定报头
        mqttWriter.writeByte(getFixedHeaderByte1(fixedHeader));
        mqttWriter.write(encodeMBI(remainingLength));


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

        //第三部分：有效载荷
        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        mqttWriter.write(clientIdBytes);

        //遗嘱
        if (willTopicBytes != null) {
            mqttWriter.write(willTopicBytes);
            mqttWriter.writeShort((short) mqttConnectPayload.willMessageInBytes().length);
            mqttWriter.write(mqttConnectPayload.willMessageInBytes());
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

    public ConnectProperties getConnectProperties() {
        ValidateUtils.isTrue(version == MqttVersion.MQTT_5, "");
        return connectProperties;
    }
}
