package org.smartboot.mqtt.common.message.properties;

import org.smartboot.mqtt.common.message.MqttMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/4
 */
public class UserProperty {
    private final String key;
    private final String value;

    private byte[] keyBytes;
    private byte[] valueBytes;

    public UserProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void decode() {
        keyBytes = MqttMessage.encodeUTF8(key);
        valueBytes = MqttMessage.encodeUTF8(value);
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public byte[] getValueBytes() {
        return valueBytes;
    }
}
