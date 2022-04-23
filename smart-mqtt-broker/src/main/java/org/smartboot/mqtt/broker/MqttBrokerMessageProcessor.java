package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.processor.ConnectProcessor;
import org.smartboot.mqtt.broker.processor.DisConnectProcessor;
import org.smartboot.mqtt.broker.processor.MqttAckProcessor;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.processor.PingReqProcessor;
import org.smartboot.mqtt.broker.processor.PublishProcessor;
import org.smartboot.mqtt.broker.processor.SubscribeProcessor;
import org.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.exception.MqttProcessException;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPubAckMessage;
import org.smartboot.mqtt.common.message.MqttPubCompMessage;
import org.smartboot.mqtt.common.message.MqttPubRecMessage;
import org.smartboot.mqtt.common.message.MqttPubRelMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.message.MqttSubscribeMessage;
import org.smartboot.mqtt.common.message.MqttUnsubscribeMessage;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttBrokerMessageProcessor extends AbstractMessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttBrokerMessageProcessor.class);
    /**
     * Mqtt服务全局Context
     */
    private final BrokerContext mqttContext;
    /**
     * 处于在线状态的会话
     */
    private final Map<String, MqttSession> onlineSessions = new ConcurrentHashMap<>();
    private final Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();
    private final QosPublisher qosPublisher = new QosPublisher();

    {
        processorMap.put(MqttPingReqMessage.class, new PingReqProcessor());
        processorMap.put(MqttConnectMessage.class, new ConnectProcessor());
        processorMap.put(MqttPublishMessage.class, new PublishProcessor());
        processorMap.put(MqttSubscribeMessage.class, new SubscribeProcessor());
        processorMap.put(MqttUnsubscribeMessage.class, new UnSubscribeProcessor());
        processorMap.put(MqttPubAckMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttPubRelMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttPubRecMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttPubCompMessage.class, new MqttAckProcessor<>());
        processorMap.put(MqttDisconnectMessage.class, new DisConnectProcessor());
    }

    public MqttBrokerMessageProcessor(BrokerContext mqttContext) {
        this.mqttContext = mqttContext;
        addPlugin(new MonitorPlugin<>(5));
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("smart-mqtt broker process messageClass:{} ,data:{}", msg.getClass().getSimpleName(), msg);
        }
        MqttProcessor processor = processorMap.get(msg.getClass());
        if (processor != null) {
            MqttSession mqttSession = onlineSessions.get(session.getSessionID());
            mqttSession.getListeners().forEach(listener -> listener.onMessageReceived(mqttSession, msg));
            mqttSession.setLatestReceiveMessageTime(System.currentTimeMillis());
            processor.process(mqttContext, mqttSession, msg);
        } else {
            System.err.println("unSupport message: " + msg);
        }
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                MqttSession mqttSession = new MqttSession(mqttContext, session, qosPublisher);
                //注册MqttSessionListener
                mqttContext.getListeners().getSessionListeners().forEach(listener -> {
                    mqttSession.addListener(listener);
                    listener.onSessionCreate(mqttSession);
                });

                onlineSessions.put(session.getSessionID(), mqttSession);
                break;
            case SESSION_CLOSED:
                onlineSessions.remove(session.getSessionID()).disconnect();
                break;
            case PROCESS_EXCEPTION:
                if (throwable instanceof MqttProcessException) {
                    LOGGER.warn("process exception", throwable);
                    ((MqttProcessException) throwable).getCallback().run();
                }
                break;
        }
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
