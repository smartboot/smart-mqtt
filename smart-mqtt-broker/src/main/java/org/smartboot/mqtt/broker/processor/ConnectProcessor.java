package org.smartboot.mqtt.broker.processor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttProtocolEnum;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnAckVariableHeader;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttConnectPayload;
import org.smartboot.mqtt.common.message.MqttConnectVariableHeader;
import org.smartboot.mqtt.common.message.MqttFixedHeader;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        //有效性校验
        //服务端必须按照 3.1 节的要求验证 CONNECT 报文，如果报文不符合规范，服务端不发送CONNACK 报文直接关闭网络连接
        checkMessage(session, mqttConnectMessage);

        //身份验证
        ValidateUtils.isTrue(login(context, session, mqttConnectMessage), "login fail", session::close);

        //清理会话
        refreshSession(context, session, mqttConnectMessage);

        //保持连接状态监听
        initializeKeepAliveTimeout(context, session, mqttConnectMessage);

        //存储遗嘱消息
        storeWillMessage(session, mqttConnectMessage);

        //如果服务端收到清理会话（CleanSession）标志为 1 的连接，除了将 CONNACK 报文中的返回码设置为 0 之外，
        // 还必须将 CONNACK 报文中的当前会话设置（Session Present）标志为 0。
        //如果服务端收到一个 CleanSession 为 0 的连接，当前会话标志的值取决于服务端是否已经保存了 ClientId 对应客户端的会话状态。
        // 如果服务端已经保存了会话状态，它必须将 CONNACK 报文中的当前会话标志设置为 1 。
        // 如果服务端没有已保存的会话状态，它必须将 CONNACK 报文中的当前会话设置为 0。还需要将 CONNACK 报文中的返回码设置为 0
        MqttConnAckMessage mqttConnAckMessage = connAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, !mqttConnectMessage.getVariableHeader().isCleanSession());
        session.write(mqttConnAckMessage);
//        LOGGER.info("CONNECT message processed CId={}, username={}", clientId, payload.userName());
    }

    private void initializeKeepAliveTimeout(BrokerContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        //如果保持连接的值非零，并且服务端在一点五倍的保持连接时间内没有收到客户端的控制报文，
        // 它必须断开客户端的网络连接，认为网络连接已断开.
        int timeout = mqttConnectMessage.getVariableHeader().keepAliveTimeSeconds() * 1000;
        if (timeout > 0) {
            timeout += timeout >> 1;
        }
        final long finalTimeout = (timeout == 0 || timeout > context.getBrokerConfigure().getMaxKeepAliveTime()) ? context.getBrokerConfigure().getMaxKeepAliveTime() : timeout;
        context.getKeepAliveThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                if (session.isClosed()) {
                    LOGGER.warn("session:{} is closed, quit keepalive monitor.", session.getClientId());
                    return;
                }
                long remainingTime = finalTimeout + session.getLatestReceiveMessageTime() - System.currentTimeMillis();
                if (remainingTime > 0) {
//                    LOGGER.info("continue monitor, wait:{},current:{} latestReceiveTime:{} timeout:{}", remainingTime, System.currentTimeMillis(), session.getLatestReceiveMessageSecondTime(), finalTimeout);
                    context.getKeepAliveThreadPool().schedule(this, remainingTime, TimeUnit.MILLISECONDS);
                } else {
                    LOGGER.info("session:{} keepalive timeout,current:{} latestReceiveTime:{} timeout:{}", session.getClientId(), System.currentTimeMillis(), session.getLatestReceiveMessageTime(), finalTimeout);
                    session.close();
                }
            }
        }, finalTimeout, TimeUnit.MILLISECONDS);
    }

    private void checkMessage(MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectVariableHeader connectVariableHeader = mqttConnectMessage.getVariableHeader();
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文。
        final MqttProtocolEnum protocol = MqttProtocolEnum.getByName(connectVariableHeader.protocolName());
        ValidateUtils.notNull(protocol, "invalid protocol", () -> {
            LOGGER.error("invalid protocol:{}", connectVariableHeader.protocolName());
            session.close();
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
            session.close();
        });

        //服务端必须验证 CONNECT 控制报文的保留标志位（第 0 位）是否为 0，如果不为 0 必须断开客户端连接。
        ValidateUtils.isTrue(connectVariableHeader.getReserved() == 0, "", session::close);

        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        //服务端必须允许 1 到 23 个字节长的 UTF-8 编码的客户端标识符，客户端标识符只能包含这些字符：
        //“0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ”（大写字母，小写字母和数字）
        boolean invalidClient = StringUtils.isNotBlank(clientId) && (mqttVersion == MqttVersion.MQTT_3_1 && clientId.length() > MqttCodecUtil.MAX_CLIENT_ID_LENGTH);
        ValidateUtils.isTrue(!invalidClient, "", () -> {
            MqttConnAckMessage connAckMessage = connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);
            session.write(connAckMessage);
            session.close();
            LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
        });
        //如果客户端提供的 ClientId 为零字节且清理会话标志为 0，
        // 服务端必须发送返回码为 0x02（表示标识符不合格）的 CONNACK 报文响应客户端的 CONNECT 报文，然后关闭网络连接
        ValidateUtils.isTrue(connectVariableHeader.isCleanSession() || StringUtils.isBlank(clientId), "", () -> {
            MqttConnAckMessage connAckMessage = connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);
            session.write(connAckMessage);
            session.close();
            LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
        });
    }

    private void refreshSession(BrokerContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectPayload payload = mqttConnectMessage.getPayload();
        String clientId = payload.clientIdentifier();

        if (mqttConnectMessage.getVariableHeader().isCleanSession()) {
            if (StringUtils.isBlank(clientId)) {
                clientId = UUID.randomUUID().toString().replace("-", "");
                LOGGER.info("Client has connected with a server generated identifier. CId={}, username={}", clientId, payload.userName());
            } else {
                //如果清理会话（CleanSession）标志被设置为 1，客户端和服务端必须丢弃之前的任何会话并开始一个新的会话。
                // 会话仅持续和网络连接同样长的时间。与这个会话关联的状态数据不能被任何之后的会话重用
                MqttSession mqttSession = context.getSession(clientId);
                if (mqttSession != null) {
                    mqttSession.close();
                    LOGGER.info("clean session:{}", clientId);
                }
            }
        } else {
            //如果清理会话（CleanSession）标志被设置为 0，服务端必须基于当前会话（使用客户端标识符识别）的
            //状态恢复与客户端的通信。如果没有与这个客户端标识符关联的会话，服务端必须创建一个新的会话。
            MqttSession mqttSession = context.getSession(clientId);
            if (mqttSession != null) {
                LOGGER.info("Client ID is being used in an existing connection, force to be closed. CId={}", clientId);
                mqttSession.close();
            }
        }
        session.setClientId(clientId);
        context.addSession(session);
        LOGGER.info("add session for client:{}", clientId);
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
        StoredMessage willMessage = new StoredMessage(msg.getPayload().willMessageInBytes(), MqttQoS.valueOf(msg.getVariableHeader().willQos()), msg.getPayload().willTopic());
        willMessage.setRetained(msg.getMqttFixedHeader().isRetain());
        session.setWillMessage(willMessage);
    }

    private MqttConnAckMessage connFailAck(MqttConnectReturnCode returnCode) {
        //如果服务端发送了一个包含非零返回码的 CONNACK 报文，它必须将当前会话标志设置为 0
        ValidateUtils.isTrue(returnCode != CONNECTION_ACCEPTED, "");
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }
}
