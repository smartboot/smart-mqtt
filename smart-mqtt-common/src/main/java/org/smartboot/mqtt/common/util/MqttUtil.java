package org.smartboot.mqtt.common.util;

import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.UUID;

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

    public static MqttPublishMessage createPublishMessage(int packetId, String topic, MqttQoS subscribeQos, byte[] payload) {
        return MqttMessageBuilders.publish().payload(payload).qos(subscribeQos).packetId(packetId).topicName(topic).build();
    }

    public static String createClientId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
