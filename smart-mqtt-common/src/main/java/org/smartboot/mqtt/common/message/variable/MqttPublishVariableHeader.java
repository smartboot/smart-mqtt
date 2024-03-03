/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
    private final byte[] encodedTopic;

    public MqttPublishVariableHeader(int packetId, String topicName, byte[] encodedTopic, PublishProperties properties) {
        super(packetId, properties);
        this.topicName = topicName;
        this.encodedTopic = encodedTopic;
    }

    public String getTopicName() {
        return topicName;
    }

    public byte[] getEncodedTopic() {
        return encodedTopic;
    }

    @Override
    protected int preEncode0() {
        int length = getPacketId() > 0 ? 2 : 0;
        length += encodedTopic.length;
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.write(encodedTopic);
        if (getPacketId() > 0) {
            MqttCodecUtil.writeMsbLsb(mqttWriter, getPacketId());
        }
        if (properties != null) {
            properties.writeTo(mqttWriter);
        }
    }
}
