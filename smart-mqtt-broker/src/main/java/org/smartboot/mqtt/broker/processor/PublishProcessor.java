package org.smartboot.mqtt.broker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubAckVariableHeader;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.function.Consumer;

/**
 * 发布Topic
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PublishProcessor extends AuthorizedMqttProcessor<MqttPublishMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProcessor.class);


    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
//        LOGGER.info("receive publish message:{}", mqttPublishMessage);

        MqttQoS mqttQoS = mqttPublishMessage.getFixedHeader().getQosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                processQos0(context, session, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                processQos1(context, session, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                processQos2(context, session, mqttPublishMessage);
                break;
            default:
                LOGGER.warn("unsupport mqttQos:{}", mqttQoS);
                break;
        }

    }

    private void processQos0(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        // 消息投递至消息总线
        context.getEventBus().publish(ServerEventType.RECEIVE_PUBLISH_MESSAGE, EventObject.newEventObject(session, mqttPublishMessage));
    }

    private void processQos1(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {

        final int messageId = mqttPublishMessage.getVariableHeader().getPacketId();

        //给 publisher 回响应
        MqttPubAckVariableHeader variableHeader = new MqttPubAckVariableHeader(messageId, null);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(variableHeader);
        session.write(pubAckMessage);

        // 消息投递至消息总线
        context.getEventBus().publish(ServerEventType.RECEIVE_PUBLISH_MESSAGE, EventObject.newEventObject(session, mqttPublishMessage));
    }

    private void processQos2(BrokerContext context, MqttSession session, MqttPublishMessage mqttPublishMessage) {
        //暂存publishMessage

        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(mqttPublishMessage.getVariableHeader().getPacketId());
        //响应监听
        session.write(pubRecMessage, (Consumer<MqttPubRelMessage>) message -> {
            //发送pubRel消息。
            MqttPubCompMessage pubRelMessage = new MqttPubCompMessage(message.getVariableHeader().getPacketId());
            session.write(pubRelMessage);
            // 消息投递至消息总线
            context.getEventBus().publish(ServerEventType.RECEIVE_PUBLISH_MESSAGE, EventObject.newEventObject(session, mqttPublishMessage));
        });
    }
}
