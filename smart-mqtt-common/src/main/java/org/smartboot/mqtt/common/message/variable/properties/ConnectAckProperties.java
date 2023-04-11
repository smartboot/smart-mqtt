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
 * @version V1.0 , 2023/1/6
 */
public class ConnectAckProperties extends AbstractProperties {
    private static final int PROPERTIES_BITS = SESSION_EXPIRY_INTERVAL_BIT | RECEIVE_MAXIMUM_BIT | MAXIMUM_QOS_BIT | RETAIN_AVAILABLE_BIT | MAXIMUM_PACKET_SIZE_BIT | ASSIGNED_CLIENT_IDENTIFIER_BIT | TOPIC_ALIAS_MAXIMUM_BIT | REASON_STRING_BIT | USER_PROPERTY_BIT | WILDCARD_SUBSCRIPTION_AVAILABLE_BIT | SUBSCRIPTION_IDENTIFIER_AVAILABLE_BIT | SHARED_SUBSCRIPTION_AVAILABLE_BIT | SERVER_KEEP_ALIVE_BIT | RESPONSE_INFORMATION_BIT | SERVER_REFERENCE_BIT | AUTHENTICATION_METHOD_BIT | AUTHENTICATION_DATA_BIT;

    public ConnectAckProperties() {
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

    public int getMaximumQoS() {
        return properties.getMaximumQoS();
    }

    public void setMaximumQoS(byte maximumQoS) {
        properties.setMaximumQoS(maximumQoS);
    }

    public byte getRetainAvailable() {
        return properties.getRetainAvailable();
    }

    public void setRetainAvailable(byte retainAvailable) {
        properties.setRetainAvailable(retainAvailable);
    }

    public Integer getMaximumPacketSize() {
        return properties.getMaximumPacketSize();
    }

    public void setMaximumPacketSize(Integer maximumPacketSize) {
        properties.setMaximumPacketSize(maximumPacketSize);
    }

    public String getAssignedClientIdentifier() {
        return properties.getAssignedClientIdentifier();
    }

    public void setAssignedClientIdentifier(String assignedClientIdentifier) {
        properties.setAssignedClientIdentifier(assignedClientIdentifier);
    }

    public int getTopicAliasMaximum() {
        return properties.getTopicAliasMaximum();
    }

    public void setTopicAliasMaximum(int topicAliasMaximum) {
        properties.setTopicAliasMaximum(topicAliasMaximum);
    }

    public String getReasonString() {
        return properties.getReasonString();
    }

    public void setReasonString(String reasonString) {
        properties.setReasonString(reasonString);
    }

    public List<UserProperty> getUserProperties() {
        return properties.getUserProperties();
    }

    public byte getWildcardSubscriptionAvailable() {
        return properties.getWildcardSubscriptionAvailable();
    }

    public void setWildcardSubscriptionAvailable(byte wildcardSubscriptionAvailable) {
        properties.setWildcardSubscriptionAvailable(wildcardSubscriptionAvailable);
    }

    public byte getSubscriptionIdentifierAvailable() {
        return properties.getSubscriptionIdentifierAvailable();
    }

    public void setSubscriptionIdentifierAvailable(byte subscriptionIdentifierAvailable) {
        properties.setSubscriptionIdentifierAvailable(subscriptionIdentifierAvailable);
    }

    public byte getSharedSubscriptionAvailable() {
        return properties.getSharedSubscriptionAvailable();
    }

    public void setSharedSubscriptionAvailable(byte sharedSubscriptionAvailable) {
        properties.setSharedSubscriptionAvailable(sharedSubscriptionAvailable);
    }

    public int getServerKeepAlive() {
        return properties.getServerKeepAlive();
    }

    public void setServerKeepAlive(int serverKeepAlive) {
        properties.setServerKeepAlive(serverKeepAlive);
    }

    public String getResponseInformation() {
        return properties.getResponseInformation();
    }

    public void setResponseInformation(String responseInformation) {
        properties.setResponseInformation(responseInformation);
    }

    public String getServerReference() {
        return properties.getServerReference();
    }

    public void setServerReference(String serverReference) {
        properties.setServerReference(serverReference);
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
}
