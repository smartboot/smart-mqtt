/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.payload;

import org.smartboot.mqtt.common.MqttWriter;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.Codec;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.variable.properties.WillProperties;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/6
 */
public class WillMessage extends Codec {
    /**
     * 遗嘱Topic
     */
    private String topic = null;
    private byte[] topicBytes;
    /**
     * 遗嘱消息内容
     */
    private byte[] payload;
    /**
     * 遗嘱消息等级
     */
    private MqttQoS willQos;

    private boolean retained;

    private WillProperties properties;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    void check() {
        ValidateUtils.notBlank(topic, "topic is null");
        ValidateUtils.notNull(willQos, "qos is null");
        ValidateUtils.notNull(payload, "payload is null");
    }

    public MqttQoS getWillQos() {
        return willQos;
    }

    public void setWillQos(MqttQoS willQos) {
        this.willQos = willQos;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public WillProperties getProperties() {
        return properties;
    }

    public void setProperties(WillProperties properties) {
        this.properties = properties;
    }

    protected int preEncode() {
        topicBytes = MqttCodecUtil.encodeUTF8(topic);
        int length = topicBytes.length + 2 + payload.length;
        if (properties != null) {
            length += properties.preEncode();
        }
        return length;
    }

    protected void writeTo(MqttWriter writer) throws IOException {
        if (properties != null) {
            properties.writeTo(writer);
        }
        writer.write(topicBytes);
        MqttCodecUtil.writeMsbLsb(writer, payload.length);
        writer.write(payload);
    }
}
