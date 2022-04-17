package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.processor.ConnectProcessor;
import org.smartboot.mqtt.broker.processor.MqttAckProcessor;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.processor.PingReqProcessor;
import org.smartboot.mqtt.broker.processor.PublishProcessor;
import org.smartboot.mqtt.broker.processor.SubscribeProcessor;
import org.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import org.smartboot.mqtt.common.QosPublisher;
import org.smartboot.mqtt.common.exception.MqttProcessException;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
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
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    }

    public MqttBrokerMessageProcessor(BrokerContext mqttContext) {
        this.mqttContext = mqttContext;
//        addPlugin(new StreamMonitorPlugin<>());
    }

    @Override
    public void process0(AioSession session, MqttMessage msg) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("process msg:{}", msg);
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
                //网络连接建立后，如果服务端在合理的时间内没有收到 CONNECT 报文，服务端应该关闭这个连接。
                MqttSession mqttSession = new MqttSession(mqttContext, session, qosPublisher);
                QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                    if (!mqttSession.isAuthorized()) {
                        mqttSession.close();
                    }
                }, mqttContext.getBrokerConfigure().getNoConnectIdleTimeout(), TimeUnit.MILLISECONDS);
                onlineSessions.put(session.getSessionID(), mqttSession);
                break;
            case SESSION_CLOSED:
                onlineSessions.remove(session.getSessionID()).close();
                break;
            case PROCESS_EXCEPTION:
                if (throwable instanceof MqttProcessException) {
                    LOGGER.warn("process exception", throwable);
                    ((MqttProcessException) throwable).getCallback().run();
                }
                break;
        }
        System.out.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
