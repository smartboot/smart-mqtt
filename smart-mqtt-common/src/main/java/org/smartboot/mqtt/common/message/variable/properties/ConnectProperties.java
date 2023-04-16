/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable.properties;

import java.util.List;

import static org.smartboot.mqtt.common.util.MqttPropertyConstant.*;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/2
 */
public class ConnectProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = SESSION_EXPIRY_INTERVAL_BIT
            | RECEIVE_MAXIMUM_BIT | MAXIMUM_PACKET_SIZE_BIT
            | TOPIC_ALIAS_MAXIMUM_BIT | REQUEST_RESPONSE_INFORMATION_BIT
            | REQUEST_PROBLEM_INFORMATION_BIT | USER_PROPERTY_BIT | AUTHENTICATION_METHOD_BIT | AUTHENTICATION_DATA_BIT;

    public ConnectProperties() {
        super(PROPERTIES_BITS);
    }

    public int getSessionExpiryInterval() {
        return properties.getSessionExpiryInterval();
    }

    public void setSessionExpiryInterval(int sessionExpiryInterval) {
        properties.setSessionExpiryInterval(sessionExpiryInterval);
    }

    public int getReceiveMaximum() {
        return properties.getReceiveMaximum();
    }

    public void setReceiveMaximum(int receiveMaximum) {
        properties.setReceiveMaximum(receiveMaximum);
    }

    public Integer getMaximumPacketSize() {
        return properties.getMaximumPacketSize();
    }

    public void setMaximumPacketSize(Integer maximumPacketSize) {
        properties.setMaximumPacketSize(maximumPacketSize);
    }

    public int getTopicAliasMaximum() {
        return properties.getTopicAliasMaximum();
    }

    public void setTopicAliasMaximum(int topicAliasMaximum) {
        properties.setTopicAliasMaximum(topicAliasMaximum);
    }

    public byte getRequestResponseInformation() {
        return properties.getRequestResponseInformation();
    }

    public void setRequestResponseInformation(byte requestResponseInformation) {
        properties.setRequestResponseInformation(requestResponseInformation);
    }

    public byte getRequestProblemInformation() {
        return properties.getRequestProblemInformation();
    }

    public void setRequestProblemInformation(byte requestProblemInformation) {
        properties.setRequestProblemInformation(requestProblemInformation);
    }


    public String getAuthenticationMethod() {
        return properties.getAuthenticationMethod();
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        properties.setAuthenticationMethod(authenticationMethod);
    }

    public byte[] getAuthenticationData() {
        return properties.getAuthenticationData();
    }

    public void setAuthenticationData(byte[] authenticationData) {
        properties.setAuthenticationData(authenticationData);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }
}
