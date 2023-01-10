package org.smartboot.mqtt.common.message;

/**
 * CONNECT 报文的有效载荷（payload）包含一个或多个以长度为前缀的字段，
 * 可变报头中的标志决定是否包含这些字段。
 * 如果包含的话，必须按这个顺序出现：客户端标识符，遗嘱主题，遗嘱消息，用户名，密 码
 */
public final class MqttConnectPayload {

    /**
     * 客户端标识符
     */
    private final String clientIdentifier;

    /**
     * 遗嘱消息
     */
    private final WillMessage willMessage;
    /**
     * 用户名
     */
    private final String userName;
    /**
     * 密码
     */
    private final byte[] password;


    public MqttConnectPayload(String clientIdentifier, WillMessage willMessage, String userName, byte[] password) {
        this.clientIdentifier = clientIdentifier;
        this.willMessage = willMessage;
        this.userName = userName;
        this.password = password;
    }

    public String clientIdentifier() {
        return clientIdentifier;
    }

    public WillMessage getWillMessage() {
        return willMessage;
    }

    public String userName() {
        return userName;
    }

    public byte[] passwordInBytes() {
        return password;
    }


}