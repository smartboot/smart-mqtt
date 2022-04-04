package org.smartboot.mqtt.common.util;

import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/29
 */
public class MqttUtil {
    /**
     * Topic 通配符
     */
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};

    public static boolean containsTopicWildcards(String topicName) {
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static MqttPublishMessage createPublishMessage(int packetId, StoredMessage storedMessage, MqttQoS subscribeQos) {
        return MqttMessageBuilders.publish().payload(storedMessage.getPayload()).qos(storedMessage.getMqttQoS().value() > subscribeQos.value() ? subscribeQos : storedMessage.getMqttQoS()).packetId(packetId).topicName(storedMessage.getTopic()).build();
    }
}
