package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class ConnectAckProperties {
    /**
     * 会话过期间隔
     */
    private int sessionExpiryInterval;

    /**
     * 接收最大值只将被应用在当前网络连接。如果没有设置最大接收值，将使用默认值65535。
     */
    private int receiveMaximum = 65535;

    /**
     * 最大服务质量
     */
    private int maximumQoS;

    /**
     * 保留可用
     */
    private byte retainAvailable;

    /**
     * 最大报文长度
     */
    private Integer maximumPacketSize;

    /**
     * 分配客户标识符
     */
    private String assignedClientIdentifier;

    /**
     * 主题别名最大值
     */
    private int topicAliasMaximum;

    /**
     * 原因字符串
     */
    private String reasonString;

    /**
     * 用户属性
     */
    private final List<UserProperty> userProperties = new ArrayList<>();

    /**
     * 通配符订阅可用
     */
    private byte wildcardSubscriptionAvailable;

    /**
     * 订阅标识符可用
     */
    private byte subscriptionIdentifierAvailable;

    /**
     * 共享订阅可用
     */
    private byte sharedSubscriptionAvailable;

    /**
     * 服务端保持连接
     */
    private int serverKeepAlive;

    /**
     * 响应信息
     */
    private String responseInformation;

    /**
     * 服务端参考
     */
    private String serverReference;

    /**
     * 认证方法
     */
    private String authenticationMethod;

    /**
     * 认证数据
     */
    private byte[] authenticationData;

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

    public int getMaximumQoS() {
        return maximumQoS;
    }

    public void setMaximumQoS(int maximumQoS) {
        this.maximumQoS = maximumQoS;
    }

    public byte getRetainAvailable() {
        return retainAvailable;
    }

    public void setRetainAvailable(byte retainAvailable) {
        this.retainAvailable = retainAvailable;
    }

    public Integer getMaximumPacketSize() {
        return maximumPacketSize;
    }

    public void setMaximumPacketSize(Integer maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    public String getAssignedClientIdentifier() {
        return assignedClientIdentifier;
    }

    public void setAssignedClientIdentifier(String assignedClientIdentifier) {
        this.assignedClientIdentifier = assignedClientIdentifier;
    }

    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    public void setTopicAliasMaximum(int topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
    }

    public String getReasonString() {
        return reasonString;
    }

    public void setReasonString(String reasonString) {
        this.reasonString = reasonString;
    }

    public List<UserProperty> getUserProperties() {
        return userProperties;
    }

    public byte getWildcardSubscriptionAvailable() {
        return wildcardSubscriptionAvailable;
    }

    public void setWildcardSubscriptionAvailable(byte wildcardSubscriptionAvailable) {
        this.wildcardSubscriptionAvailable = wildcardSubscriptionAvailable;
    }

    public byte getSubscriptionIdentifierAvailable() {
        return subscriptionIdentifierAvailable;
    }

    public void setSubscriptionIdentifierAvailable(byte subscriptionIdentifierAvailable) {
        this.subscriptionIdentifierAvailable = subscriptionIdentifierAvailable;
    }

    public byte getSharedSubscriptionAvailable() {
        return sharedSubscriptionAvailable;
    }

    public void setSharedSubscriptionAvailable(byte sharedSubscriptionAvailable) {
        this.sharedSubscriptionAvailable = sharedSubscriptionAvailable;
    }

    public int getServerKeepAlive() {
        return serverKeepAlive;
    }

    public void setServerKeepAlive(int serverKeepAlive) {
        this.serverKeepAlive = serverKeepAlive;
    }

    public String getResponseInformation() {
        return responseInformation;
    }

    public void setResponseInformation(String responseInformation) {
        this.responseInformation = responseInformation;
    }

    public String getServerReference() {
        return serverReference;
    }

    public void setServerReference(String serverReference) {
        this.serverReference = serverReference;
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
