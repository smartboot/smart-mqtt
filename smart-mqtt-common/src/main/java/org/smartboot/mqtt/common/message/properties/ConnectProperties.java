package org.smartboot.mqtt.common.message.properties;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/2
 */
public class ConnectProperties {
    private final MqttProperties properties;

    public ConnectProperties(MqttProperties properties) {
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
