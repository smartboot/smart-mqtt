package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/2
 */
public class ConnectProperties {
    /**
     * 会话过期间隔.
     * 如果会话过期间隔（Session Expiry Interval）值未指定，则使用0。
     * 如果设置为0或者未指定，会话将在网络连接（Network Connection）关闭时结束
     */
    private int sessionExpiryInterval;
    /**
     * 接收最大值只将被应用在当前网络连接。如果没有设置最大接收值，将使用默认值65535。
     */
    private int receiveMaximum = 65535;
    /**
     * 最大报文长度
     */
    private Integer maximumPacketSize;

    /**
     * 主题别名最大值
     * 没有设置主题别名最大值属性的情况下，主题别名最大值默认为零。
     */
    private int topicAliasMaximum = 0;

    /**
     * 请求响应信息
     * 如果没有请求响应信息（Request Response Information），则请求响应默认值为0
     */
    private byte requestResponseInformation;
    /**
     * 请求问题信息
     * 如果没有请求问题信息（Request Problem Information），则请求问题默认值为1
     */
    private byte requestProblemInformation = 1;

    /**
     * 用户属性
     */
    private Map<String, String> userProperty;

    /**
     * 认证方法
     */
    private String authenticationMethod;

    /**
     * 认证数据
     */
    private byte[] authenticationData;

    /**
     * 用户属性
     */
    private final List<UserProperty> userProperties = new ArrayList<>();

    public int getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(int sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    public void setReceiveMaximum(int receiveMaximum) {
        this.receiveMaximum = receiveMaximum;
    }

    public Integer getMaximumPacketSize() {
        return maximumPacketSize;
    }

    public void setMaximumPacketSize(Integer maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    public void setTopicAliasMaximum(int topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
    }

    public byte getRequestResponseInformation() {
        return requestResponseInformation;
    }

    public void setRequestResponseInformation(byte requestResponseInformation) {
        this.requestResponseInformation = requestResponseInformation;
    }

    public byte getRequestProblemInformation() {
        return requestProblemInformation;
    }

    public void setRequestProblemInformation(byte requestProblemInformation) {
        this.requestProblemInformation = requestProblemInformation;
    }

    public Map<String, String> getUserProperty() {
        return userProperty;
    }

    public void setUserProperty(Map<String, String> userProperty) {
        this.userProperty = userProperty;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public byte[] getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(byte[] authenticationData) {
        this.authenticationData = authenticationData;
    }

    public List<UserProperty> getUserProperties() {
        return userProperties;
    }
}
