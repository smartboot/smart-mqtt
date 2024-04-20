/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;

/**
 * Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class TopicConsumerRecord extends AbstractConsumerRecord {
    protected final MqttSession mqttSession;
    protected final MqttQoS mqttQoS;

    public TopicConsumerRecord(BrokerTopic topic, MqttSession session, TopicSubscriber topicSubscriber, long nextConsumerOffset) {
        super(topic, topicSubscriber.getTopicFilterToken(), nextConsumerOffset);
        this.mqttSession = session;
        this.mqttQoS = topicSubscriber.getMqttQoS();
    }

    /**
     * 推送消息到客户端
     */
    public void pushToClient() {
        if (mqttSession.isDisconnect() || !enable) {
            return;
        }
        int i = 0;
        while (push0()) {
            if (i++ > 100) {
                topic.addSubscriber(this);
                topic.addVersion();
                break;
            }
        }
        mqttSession.flush();
    }

    private boolean push0() {
        Message message = topic.getMessageQueue().get(nextConsumerOffset);
        if (message == null) {
            topic.addSubscriber(this);
            return false;
        }

        MqttMessageBuilders.PublishBuilder publishBuilder = MqttMessageBuilders.publish().payload(message.getPayload()).qos(mqttQoS).topic(message.getTopicBytes());
        if (mqttSession.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }

        nextConsumerOffset = message.getOffset() + 1;
        mqttSession.write(publishBuilder.build(), false);
        return true;
    }

    public final MqttSession getMqttSession() {
        return mqttSession;
    }

    public final MqttQoS getMqttQoS() {
        return mqttQoS;
    }
}
