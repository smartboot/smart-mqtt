package org.smartboot.mqtt.broker.processor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.TopicSubscriber;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.persistence.session.SessionState;
import org.smartboot.mqtt.broker.persistence.session.SessionStateProvider;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttProtocolEnum;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnAckVariableHeader;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttConnectPayload;
import org.smartboot.mqtt.common.message.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.common.util.ValidateUtils;

import static org.smartboot.mqtt.common.enums.MqttConnectReturnCode.*;

/**
 * 连接处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class ConnectProcessor implements MqttProcessor<MqttConnectMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectProcessor.class);

    @Override
    public void process(BrokerContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
//        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        //有效性校验
        //服务端必须按照 3.1 节的要求验证 CONNECT 报文，如果报文不符合规范，服务端不发送CONNACK 报文直接关闭网络连接
        checkMessage(session, mqttConnectMessage);

        //身份验证
        ValidateUtils.isTrue(login(context, session, mqttConnectMessage), "login fail", session::disconnect);

        //清理会话
        refreshSession(context, session, mqttConnectMessage);

        //存储遗嘱消息
        storeWillMessage(session, mqttConnectMessage);

        //如果服务端收到清理会话（CleanSession）标志为 1 的连接，除了将 CONNACK 报文中的返回码设置为 0 之外，
        // 还必须将 CONNACK 报文中的当前会话设置（Session Present）标志为 0。
        //如果服务端收到一个 CleanSession 为 0 的连接，当前会话标志的值取决于服务端是否已经保存了 ClientId 对应客户端的会话状态。
        // 如果服务端已经保存了会话状态，它必须将 CONNACK 报文中的当前会话标志设置为 1 。
        // 如果服务端没有已保存的会话状态，它必须将 CONNACK 报文中的当前会话设置为 0。还需要将 CONNACK 报文中的返回码设置为 0
        MqttConnAckMessage mqttConnAckMessage = connAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, !mqttConnectMessage.getVariableHeader().isCleanSession());
        session.write(mqttConnAckMessage);

        context.getEventBus().publish(ServerEventType.CONNECT, EventObject.newEventObject(session, mqttConnectMessage));
        LOGGER.info("CONNECT message processed CId={}", session.getClientId());
    }


    private void checkMessage(MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectVariableHeader connectVariableHeader = mqttConnectMessage.getVariableHeader();
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文。
        final MqttProtocolEnum protocol = MqttProtocolEnum.getByName(connectVariableHeader.protocolName());
        ValidateUtils.notNull(protocol, "invalid protocol", () -> {
            LOGGER.error("invalid protocol:{}", connectVariableHeader.protocolName());
            session.disconnect();
        });

        MqttConnectPayload payload = mqttConnectMessage.getPayload();
        String clientId = payload.clientIdentifier();

        //对于 3.1.1 版协议，协议级别字段的值是 4(0x04)。
        // 如果发现不支持的协议级别，服务端必须给发送一个返回码为 0x01（不支持的协议级别）的 CONNACK 报文响应
        //CONNECT 报文，然后断开客户端的连接
        final MqttVersion mqttVersion = MqttVersion.getByProtocolWithVersion(protocol, connectVariableHeader.getProtocolLevel());
        ValidateUtils.notNull(mqttVersion, "invalid version", () -> {
            MqttConnAckMessage badProto = connFailAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.disconnect();
        });

        //服务端必须验证 CONNECT 控制报文的保留标志位（第 0 位）是否为 0，如果不为 0 必须断开客户端连接。
        ValidateUtils.isTrue(connectVariableHeader.getReserved() == 0, "", session::disconnect);

        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        //服务端必须允许 1 到 23 个字节长的 UTF-8 编码的客户端标识符，客户端标识符只能包含这些字符：
        //“0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ”（大写字母，小写字母和数字）
        boolean invalidClient = StringUtils.isNotBlank(clientId) && (mqttVersion == MqttVersion.MQTT_3_1 && clientId.length() > MqttCodecUtil.MAX_CLIENT_ID_LENGTH);
        ValidateUtils.isTrue(!invalidClient, "", () -> {
            MqttConnAckMessage connAckMessage = connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);
            session.write(connAckMessage);
            session.disconnect();
            LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
        });
        //如果客户端提供的 ClientId 为零字节且清理会话标志为 0，
        // 服务端必须发送返回码为 0x02（表示标识符不合格）的 CONNACK 报文响应客户端的 CONNECT 报文，然后关闭网络连接
        ValidateUtils.isTrue(connectVariableHeader.isCleanSession() || !StringUtils.isBlank(clientId), "", () -> {
            MqttConnAckMessage connAckMessage = connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);
            session.write(connAckMessage);
            session.disconnect();
            LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
        });
    }

    private void refreshSession(BrokerContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectPayload payload = mqttConnectMessage.getPayload();
        session.setCleanSession(mqttConnectMessage.getVariableHeader().isCleanSession());
        String clientId = payload.clientIdentifier();
        //服务端可以允许客户端提供一个零字节的客户端标识符 (ClientId) ，如果这样做了，服务端必须将这看作特
        //殊情况并分配唯一的客户端标识符给那个客户端。然后它必须假设客户端提供了那个唯一的客户端标识符，正常处理这个 CONNECT 报文
        if (clientId.length() == 0) {
            clientId = MqttUtil.createClientId();
        }

        MqttSession mqttSession = context.getSession(clientId);
        if (mqttSession != null) {
            if (session.isCleanSession()) {
                //如果清理会话（CleanSession）标志被设置为 1，客户端和服务端必须丢弃之前的任何会话并开始一个新的会话。
                mqttSession.setCleanSession(true);
                LOGGER.info("disconnect session:{}", mqttSession);
                mqttSession.disconnect();
            } else {
                //如果mqttSession#cleanSession为false，将还原会话状态
                mqttSession.disconnect();
                //如果清理会话（CleanSession）标志被设置为 0，服务端必须基于当前会话（使用客户端标识符识别）的状态恢复与客户端的通信。
                SessionStateProvider sessionStateProvider = context.getProviders().getSessionStateProvider();
                SessionState sessionState = sessionStateProvider.get(clientId);
                if (sessionState != null) {
                    session.getResponseConsumers().putAll(sessionState.getResponseConsumers());
                    sessionState.getSubscribers().forEach(topicSubscriber -> {
                        session.subscribeTopic(new TopicSubscriber(topicSubscriber.getTopic(), session, topicSubscriber.getMqttQoS(), topicSubscriber.getNextConsumerOffset(), topicSubscriber.getRetainConsumerOffset()));
                    });
                    //客户端设置清理会话（CleanSession）标志为 0 重连时，客户端和服务端必须使用原始的报文标识符重发
                    //任何未确认的 PUBLISH 报文（如果 QoS>0）和 PUBREL 报文 [MQTT-4.4.0-1]。这是唯一要求客户端或
                    //服务端重发消息的情况。
                    session.getResponseConsumers().forEach((key, ackMessage) -> {
                        session.getResponseConsumers().put(key, ackMessage);
                        session.write(ackMessage.getOriginalMessage());
                    });
                }
            }
        }

        session.setClientId(clientId);
        context.addSession(session);
        LOGGER.info("add session for client:{}", session);
    }

    private boolean login(BrokerContext context, MqttSession session, MqttConnectMessage msg) {
        boolean ok = context.getProviders().getClientAuthorizeProvider().auth(msg.getPayload().userName(), msg.getPayload().clientIdentifier(), msg.getPayload().passwordInBytes());
        session.setAuthorized(ok);
        return true;
    }

    private void storeWillMessage(MqttSession session, MqttConnectMessage msg) {
        // 遗嘱标志（Will Flag）被设置为 1，表示如果连接请求被接受了，
        // 遗嘱（Will Message）消息必须被存储在服务端并且与这个网络连接关联。
        // 之后网络连接关闭时，服务端必须发布这个遗嘱消息，
        // 除非服务端收到 DISCONNECT 报文时删除了这个遗嘱消息
        if (!msg.getVariableHeader().isWillFlag()) {
            return;
        }
        MqttPublishMessage publishMessage = MqttMessageBuilders.publish().topicName(msg.getPayload().willTopic()).qos(MqttQoS.valueOf(msg.getVariableHeader().willQos())).payload(msg.getPayload().willMessageInBytes()).retained(msg.getFixedHeader().isRetain()).build();
        session.setWillMessage(publishMessage);
    }

    private MqttConnAckMessage connFailAck(MqttConnectReturnCode returnCode) {
        //如果服务端发送了一个包含非零返回码的 CONNACK 报文，它必须将当前会话标志设置为 0
        ValidateUtils.isTrue(returnCode != CONNECTION_ACCEPTED, "");
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttConnAckVariableHeader);
    }
}
