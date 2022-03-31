package org.smartboot.mqtt.broker.processor;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.Topic;
import org.smartboot.mqtt.broker.store.StoredMessage;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;

/**
 * PUBREC 报文是对 QoS 等级 2 的 PUBLISH 报文的响应。它是 QoS 2 等级协议交换的第二个报文。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/27
 */
public class PubRelProcessor extends AuthorizedMqttProcessor<MqttPubRelMessage> {
    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPubRelMessage mqttPubRelMessage) {
        StoredMessage message = session.pollInFightMessage(mqttPubRelMessage.getPacketId());
        ValidateUtils.notNull(message, "message is null");

        final Topic topic = context.getOrCreateTopic(message.getTopic());
        if (message.isRetained()) {
            topic.getMessagesStore().storeTopic(message);
        }
        //发送pubRel消息。
        MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0));
        pubRelMessage.setPacketId(mqttPubRelMessage.getPacketId());
        session.write(pubRelMessage);
        // 发送给subscribe
        context.publish(topic, message.getMqttQoS(), message.getPayload());
    }
}
