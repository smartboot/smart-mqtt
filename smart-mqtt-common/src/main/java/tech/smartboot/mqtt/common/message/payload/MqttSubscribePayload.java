/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message.payload;

import tech.smartboot.mqtt.common.MqttWriter;
import tech.smartboot.mqtt.common.message.MqttCodecUtil;
import tech.smartboot.mqtt.common.message.MqttTopicSubscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MqttSubscribePayload extends MqttPayload {

    private List<MqttTopicSubscription> topicSubscriptions;
    private List<byte[]> topics;

    public void setTopicSubscriptions(List<MqttTopicSubscription> topicSubscriptions) {
        this.topicSubscriptions = topicSubscriptions;
    }

    public List<MqttTopicSubscription> getTopicSubscriptions() {
        return topicSubscriptions;
    }

    @Override
    protected int preEncode() {
        int length = topicSubscriptions.size();
        topics = new ArrayList<>(topicSubscriptions.size());
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            byte[] bytes = MqttCodecUtil.encodeUTF8(topicSubscription.getTopicFilter());
            topics.add(bytes);
            length += bytes.length;
        }
        return length;
    }

    @Override
    protected void writeTo(MqttWriter mqttWriter) throws IOException {
        int i = 0;
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            mqttWriter.write(topics.get(i++));
            mqttWriter.writeByte((byte) topicSubscription.getQualityOfService().value());
        }
    }
}