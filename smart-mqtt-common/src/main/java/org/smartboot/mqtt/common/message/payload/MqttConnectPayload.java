package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;

import java.io.IOException;

/**
 * CONNECT 报文的有效载荷（payload）包含一个或多个以长度为前缀的字段，
 * 可变报头中的标志决定是否包含这些字段。
 * 如果包含的话，必须按这个顺序出现：客户端标识符，遗嘱主题，遗嘱消息，用户名，密 码
 */
public final class MqttConnectPayload extends MqttPayload {

    /**
     * 客户端标识符
     */
    private final String clientIdentifier;

    private byte[] clientIdentifierBytes;

    /**
     * 遗嘱消息
     */
    private final WillMessage willMessage;
    /**
     * 用户名
     */
    private final String userName;

    private byte[] userNameBytes;
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

    protected int preEncode() {
        int length = 0;
        clientIdentifierBytes = MqttCodecUtil.encodeUTF8(clientIdentifier);
        length += clientIdentifierBytes.length;

        if (willMessage != null) {
            length += willMessage.preEncode();
        }
        if (userName != null) {
            userNameBytes = MqttCodecUtil.encodeUTF8(userName);
            length += userNameBytes.length;
        }
        if (password != null) {
            length += 2 + password.length;
        }

        return length;
    }

    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        mqttWriter.write(clientIdentifierBytes);

        //遗嘱
        if (willMessage != null) {
            willMessage.writeTo(mqttWriter);
        }
        //用户名
        if (userNameBytes != null) {
            mqttWriter.write(userNameBytes);
        }
        //密码
        if (password != null) {
            MqttCodecUtil.writeMsbLsb(mqttWriter, password.length);
            mqttWriter.write(password);
        }
    }
}