/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable;


import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttVariableHeader;
import org.smartboot.mqtt.common.message.payload.WillMessage;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * CONNECT 报文的可变报头按下列次序包含四个字段：协议名（Protocol Name），协议级别（Protocol
 * Level），连接标志（Connect Flags）和保持连接（Keep Alive）。
 */
public final class MqttConnectVariableHeader extends MqttVariableHeader<ConnectProperties> {

    /**
     * 协议名
     */
    private final String protocolName;
    /**
     * 协议级别
     */
    private final byte protocolLevel;
    private final boolean hasUserName;
    private final boolean hasPassword;
    private final boolean isWillRetain;
    private final int willQos;
    private final boolean isWillFlag;
    private final boolean isCleanSession;
    private final int reserved;
    private final int keepAliveTimeSeconds;

    public MqttConnectVariableHeader(
            String name,
            byte protocolLevel,
            int connectFlag,
            int keepAliveTimeSeconds, ConnectProperties properties) {
        super(properties);
        this.protocolName = name;
        this.protocolLevel = protocolLevel;
        this.hasUserName = (connectFlag & 0x80) == 0x80;
        this.hasPassword = (connectFlag & 0x40) == 0x40;
        this.isWillRetain = (connectFlag & 0x20) == 0x20;
        this.willQos = (connectFlag & 0x18) >> 3;
        this.isWillFlag = (connectFlag & 0x04) == 0x04;
        this.isCleanSession = (connectFlag & 0x02) == 0x02;
        this.reserved = (connectFlag & 0x01);
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    }

    public MqttConnectVariableHeader(MqttVersion mqttVersion, boolean hasUserName, boolean hasPassword, WillMessage willMessage, boolean isCleanSession, int keepAliveTimeSeconds, ConnectProperties properties) {
        super(properties);
        this.protocolName = mqttVersion.protocolName();
        this.protocolLevel = mqttVersion.protocolLevel();
        this.hasUserName = hasUserName;
        this.hasPassword = hasPassword;
        this.isWillFlag = willMessage != null;
        this.isWillRetain = isWillFlag && willMessage.isRetained();
        this.willQos = isWillFlag ? willMessage.getWillQos().value() : 0;
        this.isCleanSession = isCleanSession;
        //服务端必须验证 CONNECT 控制报文的保留标志位（第 0 位）是否为 0，如果不为 0 必须断开客户端连接
        this.reserved = 0;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    }


    public String protocolName() {
        return protocolName;
    }

    public byte getProtocolLevel() {
        return protocolLevel;
    }

    public boolean hasUserName() {
        return hasUserName;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public boolean isWillRetain() {
        return isWillRetain;
    }

    public int willQos() {
        return willQos;
    }

    public boolean isWillFlag() {
        return isWillFlag;
    }

    public boolean isCleanSession() {
        return isCleanSession;
    }

    public int keepAliveTimeSeconds() {
        return keepAliveTimeSeconds;
    }

    public int getReserved() {
        return reserved;
    }

    protected int preEncode0() {
        return 10;
    }

    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        //协议名
        byte[] nameBytes = protocolName.getBytes(StandardCharsets.UTF_8);
        ValidateUtils.isTrue(nameBytes.length == 4, "invalid protocol name");
        mqttWriter.writeShort((short) nameBytes.length);
        mqttWriter.write(nameBytes);
        //协议级别
        mqttWriter.writeByte(protocolLevel);
        //连接标志
        byte connectFlag = 0x00;
        if (hasUserName) {
            connectFlag = (byte) 0x80;
        }
        if (hasPassword) {
            connectFlag |= 0x40;
        }
        if (isWillFlag) {
            connectFlag |= 0x04;
            connectFlag |= willQos << 3;
            if (isWillRetain) {
                connectFlag |= 0x20;
            }
        }
        if (isCleanSession) {
            connectFlag |= 0x02;
        }
        mqttWriter.writeByte(connectFlag);
        //保持连接
        mqttWriter.writeShort((short) keepAliveTimeSeconds);

        // Connect属性
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
