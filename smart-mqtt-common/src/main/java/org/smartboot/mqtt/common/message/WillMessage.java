package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.variable.properties.WillProperties;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/6
 */
public class WillMessage {
    /**
     * 遗嘱Topic
     */
    private String willTopic = null;
    private byte[] willTopicBytes;
    /**
     * 遗嘱消息内容
     */
    private byte[] willMessage;
    /**
     * 遗嘱消息等级
     */
    private MqttQoS willQos;

    private boolean isWillRetain;

    private WillProperties properties;
    private int propertiesLength;

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(byte[] willMessage) {
        this.willMessage = willMessage;
    }

    public MqttQoS getWillQos() {
        return willQos;
    }

    public void setWillQos(MqttQoS willQos) {
        this.willQos = willQos;
    }

    public boolean isWillRetain() {
        return isWillRetain;
    }

    public void setWillRetain(boolean willRetain) {
        isWillRetain = willRetain;
    }

    public WillProperties getProperties() {
        return properties;
    }

    public void setProperties(WillProperties properties) {
        this.properties = properties;
    }

    public int preEncode() {
        willTopicBytes = MqttCodecUtil.encodeUTF8(willTopic);
        int length = willMessage.length + 2 + willMessage.length;
        if (properties != null) {
            propertiesLength = properties.preEncode();
            length += MqttCodecUtil.getVariableLengthInt(propertiesLength) + properties.preEncode();
        }
        return length;
    }

    public void writeTo(MqttWriter writer) throws IOException {
        writer.write(willTopicBytes);
        MqttCodecUtil.writeMsbLsb(writer, willMessage.length);
        writer.write(willMessage);
        if (properties != null) {
            MqttCodecUtil.writeVariableLengthInt(writer, propertiesLength);
            properties.writeTo(writer);
        }
    }
}
