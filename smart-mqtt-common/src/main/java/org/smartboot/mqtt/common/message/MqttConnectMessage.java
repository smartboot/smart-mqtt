/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttProtocolEnum;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import org.smartboot.mqtt.common.message.payload.WillMessage;
import org.smartboot.mqtt.common.message.variable.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.message.variable.properties.WillProperties;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

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
        MqttConnectVariableHeader variableHeader;
        //MQTT 5.0规范
        if (version == MqttVersion.MQTT_5) {
            ConnectProperties properties = new ConnectProperties();
            properties.decode(buffer);
            variableHeader = new MqttConnectVariableHeader(protocolName, protocolLevel, b1, keepAlive, properties);
        } else {
            variableHeader = new MqttConnectVariableHeader(protocolName, protocolLevel, b1, keepAlive, null);
        }
        setVariableHeader(variableHeader);
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
            willMessage.setTopic(MqttCodecUtil.scanTopicTree(buffer, MqttCodecUtil.cache).getTopicName());
            willMessage.setPayload(MqttCodecUtil.decodeByteArray(buffer));
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
    public MqttConnectPayload getPayload() {
        return mqttConnectPayload;
    }
}
