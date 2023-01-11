package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttUnsubAckMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.mqtt.common.message.variable.MqttPubQosVariableHeader;
import org.smartboot.mqtt.common.message.variable.MqttReasonVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.ReasonProperties;

/**
 * 客户端发送 UNSUBSCRIBE 报文给服务端，用于取消订阅主题。
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class UnSubscribeProcessor extends AuthorizedMqttProcessor<MqttUnsubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnSubscribeProcessor.class);

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttUnsubscribeMessage unsubscribeMessage) {
        LOGGER.info("receive unsubscribe message:{}", unsubscribeMessage);

        //TODO
        //如果服务端删除了一个订阅：
        //  它必须停止分发任何新消息给这个客户端 [MQTT-3.10.4-2]。
        //  它必须完成分发任何已经开始往客户端发送的 QoS 1 和 QoS 2 的消息 [MQTT-3.10.4-3]。
        //  它可以继续发送任何现存的准备分发给客户端的缓存消息。
        unsubscribeMessage.getMqttUnsubscribePayload().topics()
                .forEach(session::unsubscribe);

        //当前是否存在符合条件的通配符匹配
        session.resubscribe();

        //取消订阅确认
        MqttReasonVariableHeader variableHeader = new MqttPubQosVariableHeader(unsubscribeMessage.getVariableHeader().getPacketId());
        //todo
        if (unsubscribeMessage.getVersion() == MqttVersion.MQTT_5) {
            variableHeader.setProperties(new ReasonProperties());
        }
        MqttUnsubAckMessage mqttSubAckMessage = new MqttUnsubAckMessage(variableHeader);
        session.write(mqttSubAckMessage);
    }
}
