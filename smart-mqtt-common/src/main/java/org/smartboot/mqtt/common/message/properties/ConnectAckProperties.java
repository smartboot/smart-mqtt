package org.smartboot.mqtt.common.message.properties;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class ConnectAckProperties {

    private MqttProperties properties;

    public ConnectAckProperties(MqttProperties properties) {
        this.properties = properties;
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

    public void setMaximumQoS(int maximumQoS) {
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
