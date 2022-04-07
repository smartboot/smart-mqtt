package org.smartboot.mqtt.common.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 取消订阅
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubscribeMessage extends MqttPacketIdentifierMessage {
    /**
     * UNSUBSCRIBE 报文的有效载荷包含客户端想要取消订阅的主题过滤器列表。
     */
    private MqttUnsubscribePayload mqttUnsubscribePayload;

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, int packageId, MqttUnsubscribePayload mqttUnsubscribePayload) {
        super(mqttFixedHeader, packageId);
        this.mqttUnsubscribePayload = mqttUnsubscribePayload;
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<String> unsubscribeTopics = new ArrayList<String>();
        int limit = buffer.limit();
        buffer.limit(buffer.position() + mqttFixedHeader.remainingLength() - PACKET_LENGTH);
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            unsubscribeTopics.add(decodedTopicName);
        }
        buffer.limit(limit);
        mqttUnsubscribePayload = new MqttUnsubscribePayload(unsubscribeTopics);
    }

    public MqttUnsubscribePayload getMqttUnsubscribePayload() {
        return mqttUnsubscribePayload;
    }
}
