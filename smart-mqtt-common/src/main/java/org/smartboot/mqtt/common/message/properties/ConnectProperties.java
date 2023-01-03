package org.smartboot.mqtt.common.message.properties;

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
     * 接收最大值
     */
    private Short receiveMaximum;
    /**
     * 最大报文长度
     */
    private Integer maximumPacketSize;

    /**
     * 主题别名最大值
     */
    private Short topicAliasMaximum;

    /**
     * 请求响应信息
     */
    private Integer requestResponseInformation;
    /**
     * 请求问题信息
     */
    private Integer requestProblemInformation;

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

    public Integer getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(Integer sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public Short getReceiveMaximum() {
        return receiveMaximum;
    }

    public void setReceiveMaximum(Short receiveMaximum) {
        this.receiveMaximum = receiveMaximum;
    }

    public Integer getMaximumPacketSize() {
        return maximumPacketSize;
    }

    public void setMaximumPacketSize(Integer maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    public Short getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    public void setTopicAliasMaximum(Short topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
    }

    public Integer getRequestResponseInformation() {
        return requestResponseInformation;
    }

    public void setRequestResponseInformation(Integer requestResponseInformation) {
        this.requestResponseInformation = requestResponseInformation;
    }

    public Integer getRequestProblemInformation() {
        return requestProblemInformation;
    }

    public void setRequestProblemInformation(Integer requestProblemInformation) {
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
}
