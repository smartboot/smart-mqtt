package org.smartboot.socket.mqtt.processor.server;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.common.Topic;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttPubCompMessage;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.store.StoredMessage;
import org.smartboot.socket.mqtt.util.ValidateUtils;

import java.nio.ByteBuffer;

/**
 * PUBREC 报文是对 QoS 等级 2 的 PUBLISH 报文的响应。它是 QoS 2 等级协议交换的第二个报文。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/27
 */
public class PubRelProcessor implements MqttProcessor<MqttPubRelMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubRelMessage mqttPubRelMessage) {
        StoredMessage message = session.pollInFightMessage(mqttPubRelMessage.getPacketId());
        ValidateUtils.notNull(message, "message is null");

        final Topic topic = context.getOrCreateTopic(message.getTopic());
        // 发送给subscribe
        ByteBuffer payload = ByteBuffer.wrap(message.getPayload());
        topic.getConsumerGroup().getConsumeOffsets().keySet().forEach(mqttSession -> {
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                    .payload(payload.slice())//复用内存空间
                    .qos(message.getMqttQoS())
                    .packetId(mqttPubRelMessage.getPacketId()).topicName(topic.getTopic()).build();
            mqttSession.write(publishMessage);
        });
        if (message.isRetained()) {
            topic.getMessagesStore().storeTopic(message);
        }
        //发送pubRel消息。
        MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0));
        pubRelMessage.setPacketId(mqttPubRelMessage.getPacketId());
        session.write(pubRelMessage);
    }
}
