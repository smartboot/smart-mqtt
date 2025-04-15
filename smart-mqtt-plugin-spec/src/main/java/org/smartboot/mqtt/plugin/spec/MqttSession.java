/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec;

import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.variable.MqttConnAckVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ConnectAckProperties;
import org.smartboot.mqtt.common.message.variable.properties.ConnectProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;

import static org.smartboot.mqtt.common.enums.MqttConnectReturnCode.CONNECTION_ACCEPTED;

/**
 * @author 三刀
 * @version v1.0 4/14/25
 */
public abstract class MqttSession extends AbstractSession {
    public abstract void setClientId(String clientId);

    public abstract void setCleanSession(boolean cleanSession);

    public abstract void setAuthorized(boolean authorized);

    public abstract void setProperties(ConnectProperties properties);

    public abstract boolean isCleanSession();

    public abstract MqttQoS subscribe(String topicFilter, MqttQoS mqttQoS);

    public abstract void setWillMessage(MqttPublishMessage willMessage);

    public abstract boolean isAuthorized();

    public abstract void accepted(MqttPublishMessage mqttMessage);

    public abstract void resubscribe();

    public abstract void unsubscribe(String topicFilter);

    public abstract long getLatestReceiveMessageTime();

    public static void connFailAck(MqttConnectReturnCode returnCode, MqttSession session) {
        //如果服务端发送了一个包含非零返回码的 CONNACK 报文，它必须将当前会话标志设置为 0
        ValidateUtils.isTrue(returnCode != CONNECTION_ACCEPTED, "");
        ConnectAckProperties properties = null;
        //todo
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            properties = new ConnectAckProperties();
        }
        MqttConnAckMessage badProto = connAck(returnCode, false, properties);
        session.write(badProto);
        session.disconnect();
    }

    public static MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent, ConnectAckProperties properties) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent, properties);
        return new MqttConnAckMessage(mqttConnAckVariableHeader);
    }
}
