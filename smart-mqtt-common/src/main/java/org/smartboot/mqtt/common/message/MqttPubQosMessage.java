package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubQosMessage extends MqttPacketIdentifierMessage<MqttPubQosVariableHeader> {

    public MqttPubQosMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPubQosMessage(MqttFixedHeader pubRecHeader, MqttPubQosVariableHeader variableHeader) {
        super(pubRecHeader);
        this.variableHeader = variableHeader;
    }

    @Override
    protected final void decodeVariableHeader0(ByteBuffer buffer) {
        int packetId = decodeMessageId(buffer);
        MqttPubQosVariableHeader header = null;
        if (version == MqttVersion.MQTT_5) {
            //如果剩余长度为2，则表示使用原因码0x00 （成功）
            byte reasonCode = 0;
            if (fixedHeader.remainingLength() > 2) {
                reasonCode = buffer.get();
            }
            //如果剩余长度小于4，则表示没有属性长度字段。
            if (fixedHeader.remainingLength() >= 4) {
                ReasonProperties properties = new ReasonProperties();
                properties.decode(buffer);
                header = new MqttPubQosVariableHeader(packetId, properties);
            } else {
                header = new MqttPubQosVariableHeader(packetId, null);
                header.setReasonCode(reasonCode);
            }
        } else {
            header = new MqttPubQosVariableHeader(packetId, null);
        }
        setVariableHeader(header);
    }
}
