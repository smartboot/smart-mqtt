package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishVariableHeader extends MqttPacketIdVariableHeader<PublishProperties> {

    /**
     * PUBLISH 报文中的主题名不能包含通配符
     */
    private final String topicName;
    private byte[] topicNameBytes;


    public MqttPublishVariableHeader(int packetId, String topicName, PublishProperties properties) {
        super(packetId, properties);
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    @Override
    protected int preEncode0() {
        int length = getPacketId() > 0 ? 2 : 0;
        topicNameBytes = MqttCodecUtil.encodeUTF8(topicName);
        length += topicNameBytes.length;
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.write(topicNameBytes);
        if (getPacketId() > 0) {
            MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        }
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
