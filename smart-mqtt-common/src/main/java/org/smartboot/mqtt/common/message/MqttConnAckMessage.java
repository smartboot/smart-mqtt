package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.ConnectAckProperties;
import org.smartboot.mqtt.common.message.properties.UserProperty;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttVariableMessage<MqttConnAckVariableHeader> {

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(MqttFixedHeader.CONN_ACK_HEADER);
        setVariableHeader(mqttConnAckVariableHeader);
    }

    @Override
    public void decodeVariableHeader0(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();

        //MQTT 5.0规范
        ConnectAckProperties properties = null;
        if (version == MqttVersion.MQTT_5) {
            properties = new ConnectAckProperties();
            decodeConnectAckProperties(buffer, properties);
        }
        setVariableHeader(new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent));
    }

    private void decodeConnectAckProperties(ByteBuffer buffer, ConnectAckProperties properties) {
        int remainingLength = decodeVariableByteInteger(buffer);
        if (remainingLength == 0) {
            return;
        }
        int sessionExpiryInterval = -1;
        int maximumQoS = -1;
        byte retainAvailable = -1;
        int topicAliasMaximum = -1;
        int serverKeepAlive = -1;
        byte wildcardSubscriptionAvailable = -1;
        byte subscriptionIdentifierAvailable = -1;
        byte sharedSubscriptionAvailable = -1;
        int position;
        while (remainingLength > 0) {
            position = buffer.position();
            switch (buffer.get()) {
                //会话过期间隔
                case MqttPropertyConstant.SESSION_EXPIRY_INTERVAL:
                    //包含多个会话过期间隔（Session Expiry Interval）将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(sessionExpiryInterval == -1, "");
                    sessionExpiryInterval = buffer.getInt();
                    properties.setSessionExpiryInterval(sessionExpiryInterval);
                    break;
                //接收最大值
                case MqttPropertyConstant.RECEIVE_MAXIMUM:
                    properties.setReceiveMaximum(decodeMsbLsb(buffer));
                    break;
                //最大服务质量
                case MqttPropertyConstant.MAXIMUM_QOS:
                    //包含多个最大报文长度（Maximum Packet Size）或者最大报文长度（Maximum Packet Size）值为0将造成协议错误。
                    ValidateUtils.isTrue(properties.getMaximumQoS() == -1, "");
                    maximumQoS = buffer.get();
                    properties.setMaximumQoS(maximumQoS);
                    ValidateUtils.isTrue(maximumQoS == 0 || maximumQoS == 1, "");
                    break;
                //保留可用
                case MqttPropertyConstant.RETAIN_AVAILABLE:
                    ValidateUtils.isTrue(properties.getRetainAvailable() == -1, "");
                    retainAvailable = buffer.get();
                    properties.setRetainAvailable(retainAvailable);
                    ValidateUtils.isTrue(retainAvailable == 0 || retainAvailable == 1, "");
                    break;
                //最大报文长度
                case MqttPropertyConstant.MAXIMUM_PACKET_SIZE:
                    //包含多个最大报文长度（Maximum Packet Size）或者最大报文长度（Maximum Packet Size）值为0将造成协议错误。
                    ValidateUtils.isTrue(properties.getMaximumPacketSize() == null, "");
                    int maximumPacketSize = buffer.getInt();
                    ValidateUtils.isTrue(maximumPacketSize > 0, "");
                    properties.setMaximumPacketSize(buffer.getInt());
                    break;
                //分配客户标识符
                case MqttPropertyConstant.ASSIGNED_CLIENT_IDENTIFIER:
                    //包含多个分配客户标识符将造成协议错误（Protocol Error）
                    ValidateUtils.isTrue(properties.getAssignedClientIdentifier() == null, "");
                    properties.setAssignedClientIdentifier(decodeString(buffer));
                    break;
                //主题别名最大值
                case MqttPropertyConstant.TOPIC_ALIAS_MAXIMUM:
                    //跟随其后的是用双字节整数表示的主题别名最大值（Topic Alias Maximum）。
                    // 包含多个主题别名最大值（Topic Alias Maximum）将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(topicAliasMaximum == -1, "");
                    topicAliasMaximum = decodeMsbLsb(buffer);
                    properties.setTopicAliasMaximum(decodeMsbLsb(buffer));
                    ValidateUtils.isTrue(topicAliasMaximum >= 0, "");
                    break;
                //原因字符串
                case MqttPropertyConstant.REASON_STRING:
                    //包含多个原因字符串将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(properties.getReasonString() == null, "");
                    properties.setReasonString(decodeString(buffer));
                    break;
                //用户属性
                case MqttPropertyConstant.USER_PROPERTY:
                    String key = decodeString(buffer);
                    String value = decodeString(buffer);
                    properties.getUserProperties().add(new UserProperty(key, value));
                    break;
                //通配符订阅可用
                case MqttPropertyConstant.WILDCARD_SUBSCRIPTION_AVAILABLE:
                    ValidateUtils.isTrue(properties.getWildcardSubscriptionAvailable() == -1, "");
                    wildcardSubscriptionAvailable = buffer.get();
                    properties.setWildcardSubscriptionAvailable(wildcardSubscriptionAvailable);
                    ValidateUtils.isTrue(wildcardSubscriptionAvailable == 0 || wildcardSubscriptionAvailable == 1, "");
                    break;
                //订阅标识符可用
                case MqttPropertyConstant.SUBSCRIPTION_IDENTIFIER_AVAILABLE:
                    ValidateUtils.isTrue(properties.getSubscriptionIdentifierAvailable() == -1, "");
                    subscriptionIdentifierAvailable = buffer.get();
                    properties.setSubscriptionIdentifierAvailable(subscriptionIdentifierAvailable);
                    ValidateUtils.isTrue(subscriptionIdentifierAvailable == 0 || subscriptionIdentifierAvailable == 1, "");
                    break;
                //共享订阅可用
                case MqttPropertyConstant.SHARED_SUBSCRIPTION_AVAILABLE:
                    ValidateUtils.isTrue(properties.getSharedSubscriptionAvailable() == -1, "");
                    sharedSubscriptionAvailable = buffer.get();
                    properties.setSharedSubscriptionAvailable(sharedSubscriptionAvailable);
                    ValidateUtils.isTrue(sharedSubscriptionAvailable == 0 || sharedSubscriptionAvailable == 1, "");
                    break;
                //服务端保持连接
                case MqttPropertyConstant.SERVER_KEEP_ALIVE:
                    ValidateUtils.isTrue(serverKeepAlive == -1, "");
                    serverKeepAlive = decodeMsbLsb(buffer);
                    properties.setServerKeepAlive(decodeMsbLsb(buffer));
                    ValidateUtils.isTrue(serverKeepAlive >= 0, "");
                    break;
                //响应信息
                case MqttPropertyConstant.RESPONSE_INFORMATION:
                    //包含多个响应信息将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(properties.getResponseInformation() == null, "");
                    properties.setResponseInformation(decodeString(buffer));
                    break;
                //服务端参考
                case MqttPropertyConstant.SERVER_REFERENCE:
                    //包含多个响应信息将造成协议错误（Protocol Error）。
                    ValidateUtils.isTrue(properties.getServerReference() == null, "");
                    properties.setServerReference(decodeString(buffer));
                    break;
                //认证方法
                case MqttPropertyConstant.AUTHENTICATION_METHOD:
                    //包含多个认证方法将造成协议错误
                    ValidateUtils.isTrue(properties.getAuthenticationMethod() == null, "");
                    properties.setAuthenticationMethod(decodeString(buffer));
                    break;
                //认证数据
                case MqttPropertyConstant.AUTHENTICATION_DATA:
                    //包含多个认证数据（Authentication Data）将造成协议错误
                    ValidateUtils.isTrue(properties.getAuthenticationData() == null, "");
                    byte[] data = decodeByteArray(buffer);
                    properties.setAuthenticationData(data);
                    break;
            }
            remainingLength -= buffer.position() - position;
        }
    }

    @Override
    public void writeTo(MqttWriter mqttWriter) {
        MqttConnAckVariableHeader variableHeader = getVariableHeader();
        mqttWriter.writeByte(getFixedHeaderByte1(fixedHeader));
        mqttWriter.writeByte((byte) 2);
        mqttWriter.writeByte((byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00));
        mqttWriter.writeByte(variableHeader.connectReturnCode().getCode());
    }

}
