/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable.properties;

import org.smartboot.mqtt.common.message.MqttCodecUtil;

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
        keyBytes = MqttCodecUtil.encodeUTF8(key);
        valueBytes = MqttCodecUtil.encodeUTF8(value);
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public byte[] getValueBytes() {
        return valueBytes;
    }
}
