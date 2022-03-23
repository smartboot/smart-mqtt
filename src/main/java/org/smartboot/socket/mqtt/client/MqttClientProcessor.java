package org.smartboot.socket.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttServerContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.mqtt.message.MqttPingRespMessage;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.message.MqttPubCompMessage;
import org.smartboot.socket.mqtt.message.MqttPubRecMessage;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.message.MqttSubAckMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.processor.client.ConnAckProcessor;
import org.smartboot.socket.mqtt.processor.client.PingRespProcessor;
import org.smartboot.socket.mqtt.processor.client.PubAckProcessor;
import org.smartboot.socket.mqtt.processor.client.PubCompProcessor;
import org.smartboot.socket.mqtt.processor.client.PubRecProcessor;
import org.smartboot.socket.mqtt.processor.client.PubRelProcessor;
import org.smartboot.socket.mqtt.processor.client.PublishProcessor;
import org.smartboot.socket.mqtt.processor.client.SubAckProcessor;
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
    private final MqttContext mqttContext = new MqttServerContext();
    private Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();
    private Map<String, MqttSession> sessionMap = new ConcurrentHashMap();
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
            processor.process(mqttContext, sessionMap.get(session.getSessionID()), msg);
        } else {
            LOGGER.error("unknown msg:{}", msg);
        }
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                sessionMap.put(session.getSessionID(), new MqttSession(session));
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
