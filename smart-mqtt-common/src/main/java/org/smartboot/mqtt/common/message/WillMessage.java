package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/6
 */
public class WillMessage {
    /**
     * 遗嘱Topic
     */
    private String willTopic = null;
    /**
     * 遗嘱消息内容
     */
    private byte[] willMessage;
    /**
     * 遗嘱消息等级
     */
    private MqttQoS willQos;

    private boolean isWillRetain;

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
}
