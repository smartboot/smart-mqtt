package org.smartboot.mqtt.broker.processor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.MqttContext;
import org.smartboot.mqtt.broker.MqttSession;
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

import java.nio.ByteBuffer;
import java.util.UUID;

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
    public void process(MqttContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        //有效性校验
        checkMessage(session, mqttConnectMessage);

        //身份验证
        ValidateUtils.isTrue(login(session, mqttConnectMessage), "login fail", session::close);

        //清理会话
        refreshSession(context, session, mqttConnectMessage);

//        initializeKeepAliveTimeout(channel, msg, clientId);
//        storeWillMessage(mqttConnectMessage, clientId);

        //如果服务端收到清理会话（CleanSession）标志为 1 的连接，除了将 CONNACK 报文中的返回码设置为 0 之外，
        // 还必须将 CONNACK 报文中的当前会话设置（Session Present）标志为 0。
        //如果服务端收到一个 CleanSession 为 0 的连接，当前会话标志的值取决于服务端是否已经保存了 ClientId 对应客户端的会话状态。
        // 如果服务端已经保存了会话状态，它必须将 CONNACK 报文中的当前会话标志设置为 1 。
        // 如果服务端没有已保存的会话状态，它必须将 CONNACK 报文中的当前会话设置为 0。还需要将 CONNACK 报文中的返回码设置为 0
        MqttConnAckMessage mqttConnAckMessage = connAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, !mqttConnectMessage.getVariableHeader().isCleanSession());
        session.write(mqttConnAckMessage);
//        LOGGER.info("CONNECT message processed CId={}, username={}", clientId, payload.userName());
    }

    private void checkMessage(MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectVariableHeader connectVariableHeader = mqttConnectMessage.getVariableHeader();
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文。
        final MqttProtocolEnum protocol = MqttProtocolEnum.getByName(connectVariableHeader.name());
        ValidateUtils.notNull(protocol, "invalid protocol", () -> {
            LOGGER.error("invalid protocol:{}", connectVariableHeader.name());
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

    private void refreshSession(MqttContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectPayload payload = mqttConnectMessage.getPayload();
        String clientId = payload.clientIdentifier();

        if (mqttConnectMessage.getVariableHeader().isCleanSession()) {
            if (StringUtils.isBlank(clientId)) {
                clientId = UUID.randomUUID().toString().replace("-", "");
                LOGGER.info("Client has connected with a server generated identifier. CId={}, username={}", clientId,
                        payload.userName());
            } else {
                //如果清理会话（CleanSession）标志被设置为 1，客户端和服务端必须丢弃之前的任何会话并开始一个新的会话。
                // 会话仅持续和网络连接同样长的时间。与这个会话关联的状态数据不能被任何之后的会话重用
                MqttSession mqttSession = context.getSession(clientId);
                if (mqttSession != null) {
                    mqttSession.close();
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

    private boolean login(MqttSession channel, MqttConnectMessage msg) {

        return true;
    }

    private void storeWillMessage(MqttConnectMessage msg, final String clientId) {
        // Handle will flag
        if (msg.getVariableHeader().isWillFlag()) {
            MqttQoS willQos = MqttQoS.valueOf(msg.getVariableHeader().willQos());
            LOGGER.info("Configuring MQTT last will and testament CId={}, willQos={}, willTopic={}, willRetain={}",
                    clientId, willQos, msg.getPayload().willTopic(), msg.getVariableHeader().isWillRetain());
            byte[] willPayload = msg.getPayload().willMessage().getBytes();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            // save the will testament in the clientID store
//            WillMessage will = new WillMessage(msg.payload().willTopic(), bb, msg.variableHeader().isWillRetain(),
//                    willQos);
//            m_willStore.put(clientId, will);
            LOGGER.info("MQTT last will and testament has been configured. CId={}", clientId);
        }
    }

    private MqttConnAckMessage connFailAck(MqttConnectReturnCode returnCode) {
        //如果服务端发送了一个包含非零返回码的 CONNACK 报文，它必须将当前会话标志设置为 0
        ValidateUtils.isTrue(returnCode != CONNECTION_ACCEPTED, "");
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }
}
