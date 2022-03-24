package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttUnsubAckMessage;
import org.smartboot.socket.mqtt.message.MqttUnsubscribeMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

/**
 * 客户端发送 UNSUBSCRIBE 报文给服务端，用于取消订阅主题。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class UnSubscribeProcessor implements MqttProcessor<MqttUnsubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnSubscribeProcessor.class);

    @Override
    public void process(MqttContext context, MqttSession session, MqttUnsubscribeMessage unsubscribeMessage) {
        LOGGER.info("receive unsubscribe message:{}", unsubscribeMessage);

        //TODO
        //如果服务端删除了一个订阅：
        //  它必须停止分发任何新消息给这个客户端 [MQTT-3.10.4-2]。
        //  它必须完成分发任何已经开始往客户端发送的 QoS 1 和 QoS 2 的消息 [MQTT-3.10.4-3]。
        //  它可以继续发送任何现存的准备分发给客户端的缓存消息。
        unsubscribeMessage.getMqttUnsubscribePayload().topics().forEach(topic -> context.unSubscribe(session.getClientId(), topic));

        //取消订阅确认
        MqttUnsubAckMessage mqttSubAckMessage = new MqttUnsubAckMessage(new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0));
        mqttSubAckMessage.setPacketId(mqttSubAckMessage.getPacketId());
        session.write(mqttSubAckMessage);
    }
}
