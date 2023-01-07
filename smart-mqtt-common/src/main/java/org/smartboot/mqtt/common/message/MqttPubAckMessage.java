package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.properties.MqttProperties;
import org.smartboot.mqtt.common.message.properties.ReasonProperties;
import org.smartboot.mqtt.common.util.MqttPropertyConstant;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubAckMessage extends MqttVariableMessage<MqttPubAckVariableHeader> {
    private static final int PROPERTIES_BITS = MqttPropertyConstant.REASON_STRING_BIT | MqttPropertyConstant.USER_PROPERTY_BIT;

    public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubAckMessage(MqttPubAckVariableHeader variableHeader) {
        super(MqttFixedHeader.PUB_ACK_HEADER);
        setVariableHeader(variableHeader);
    }

    @Override
    protected void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        ReasonProperties properties = null;
        byte reasonCode = 0;
        if (version == MqttVersion.MQTT_5) {
            if (fixedHeader.remainingLength() > 2) {
                reasonCode = buffer.get();
            }
            MqttProperties mqttProperties = new MqttProperties();
            mqttProperties.decode(buffer, PROPERTIES_BITS);
            properties = new ReasonProperties(mqttProperties);
//            decodeProperties(buffer, properties);
        }
        MqttPubAckVariableHeader header = new MqttPubAckVariableHeader(packetId, properties);
        header.setReasonCode(reasonCode);
        setVariableHeader(header);
    }

//    private void decodeProperties(ByteBuffer buffer, ReasonProperties properties) {
//        int remainingLength = MqttCodecUtil.decodeVariableByteInteger(buffer);
//        if (remainingLength == 0) {
//            return;
//        }
//        int position;
//        while (remainingLength > 0) {
//            position = buffer.position();
//            switch (buffer.get()) {
//                //原因字符串
//                case MqttPropertyConstant.REASON_STRING:
//                    //包含多个原因字符串将造成协议错误（Protocol Error）。
//                    ValidateUtils.isTrue(properties.getReasonString() == null, "");
//                    properties.setReasonString(decodeString(buffer));
//                    break;
//                //用户属性
//                case MqttPropertyConstant.USER_PROPERTY:
//                    String key = decodeString(buffer);
//                    String value = decodeString(buffer);
//                    properties.getUserProperties().add(new UserProperty(key, value));
//                    break;
//            }
//            remainingLength -= buffer.position() - position;
//        }
//    }

}
