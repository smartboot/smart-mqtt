package org.smartboot.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.processor.ConnAckProcessor;
import org.smartboot.mqtt.client.processor.MqttProcessor;
import org.smartboot.mqtt.client.processor.PingRespProcessor;
import org.smartboot.mqtt.client.processor.PubAckProcessor;
import org.smartboot.mqtt.client.processor.PubCompProcessor;
import org.smartboot.mqtt.client.processor.PubRecProcessor;
import org.smartboot.mqtt.client.processor.PubRelProcessor;
import org.smartboot.mqtt.client.processor.PublishProcessor;
import org.smartboot.mqtt.client.processor.SubAckProcessor;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubAckMessage;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientProcessor implements MessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClientProcessor.class);
    private Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();
    private Map<String, MqttClientSession> sessionMap = new ConcurrentHashMap();
    private MqttClient mqttClient;

    public MqttClientProcessor(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
        processorMap.put(MqttPingRespMessage.class, new PingRespProcessor());
        processorMap.put(MqttConnAckMessage.class, new ConnAckProcessor());
        processorMap.put(MqttPubAckMessage.class, new PubAckProcessor());
        processorMap.put(MqttPublishMessage.class, new PublishProcessor(mqttClient));
        processorMap.put(MqttPubRecMessage.class, new PubRecProcessor());
        processorMap.put(MqttPubCompMessage.class, new PubCompProcessor());
        processorMap.put(MqttPubRelMessage.class, new PubRelProcessor());
        processorMap.put(MqttSubAckMessage.class, new SubAckProcessor());
    }

    @Override
    public void process(AioSession session, MqttMessage msg) {
        MqttProcessor processor = processorMap.get(msg.getClass());
        if (processor != null) {
            processor.process(sessionMap.get(session.getSessionID()), msg);
        } else {
            LOGGER.error("unknown msg:{}", msg);
        }
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                sessionMap.put(session.getSessionID(), new MqttClientSession(session));
                break;
            case SESSION_CLOSED:
                mqttClient.stopPing();
                mqttClient.reconnect();
                break;
            default:
                break;
        }
        System.out.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
