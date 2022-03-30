package org.smartboot.mqtt.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.processor.ConnectProcessor;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.processor.PingReqProcessor;
import org.smartboot.mqtt.broker.processor.PubAckProcessor;
import org.smartboot.mqtt.broker.processor.PubCompProcessor;
import org.smartboot.mqtt.broker.processor.PubRecProcessor;
import org.smartboot.mqtt.broker.processor.PubRelProcessor;
import org.smartboot.mqtt.broker.processor.PublishProcessor;
import org.smartboot.mqtt.broker.processor.SubscribeProcessor;
import org.smartboot.mqtt.broker.processor.UnSubscribeProcessor;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
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
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttServerProcessor implements MessageProcessor<MqttMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttServerProcessor.class);
    /**
     * Mqtt服务全局Context
     */
    private final MqttServerContext mqttContext = new MqttServerContext();
    /**
     * 处于在线状态的会话
     */
    private final Map<String, MqttSession> onlineSessions = new ConcurrentHashMap<>();
    private final Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();

    {
        processorMap.put(MqttPingReqMessage.class, new PingReqProcessor());
        processorMap.put(MqttConnectMessage.class, new ConnectProcessor());
        processorMap.put(MqttPublishMessage.class, new PublishProcessor());
        processorMap.put(MqttSubscribeMessage.class, new SubscribeProcessor());
        processorMap.put(MqttUnsubscribeMessage.class, new UnSubscribeProcessor());
        processorMap.put(MqttPubAckMessage.class, new PubAckProcessor());
        processorMap.put(MqttPubRelMessage.class, new PubRelProcessor());
        processorMap.put(MqttPubRecMessage.class, new PubRecProcessor());
        processorMap.put(MqttPubCompMessage.class, new PubCompProcessor());
    }


    @Override
    public void process(AioSession session, MqttMessage msg) {
        LOGGER.info("process msg:{}", msg);
//        switch (msg.getMqttFixedHeader().getMessageType()) {
//            case CONNECT:
//                break;
//            case SUBSCRIBE:
//                break;
//            case UNSUBSCRIBE:
//                break;
//            case PUBLISH:
//                break;
//            case PUBREC:
//                break;
//            case PUBCOMP:
//                break;
//            case PUBREL:
//                break;
//
//            case DISCONNECT:
//                break;
//            case PUBACK:
//                break;
//            case PINGREQ:
//                break;
//            default:
//                LOGGER.error("unsupport message:{}", msg);
//
//        }
        MqttProcessor processor = processorMap.get(msg.getClass());
        if (processor != null) {
            processor.process(mqttContext, onlineSessions.get(session.getSessionID()), msg);
        } else {
            System.err.println("unSupport message: " + msg);
        }
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                onlineSessions.put(session.getSessionID(), new MqttSession(mqttContext, session));
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
