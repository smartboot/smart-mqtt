/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.processor;

import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.InflightQueue;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.enums.MqttProtocolEnum;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import tech.smartboot.mqtt.common.message.payload.WillMessage;
import tech.smartboot.mqtt.common.message.variable.MqttConnectVariableHeader;
import tech.smartboot.mqtt.common.message.variable.properties.ConnectAckProperties;
import tech.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.PublishBuilder;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.provider.SessionState;
import tech.smartboot.mqtt.plugin.spec.provider.SessionStateProvider;

import static tech.smartboot.mqtt.common.enums.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static tech.smartboot.mqtt.common.enums.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
import static tech.smartboot.mqtt.common.enums.MqttConnectReturnCode.UNSUPPORTED_PROTOCOL_VERSION;

/**
 * 连接处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class ConnectProcessor implements MqttProcessor<BrokerContextImpl, MqttConnectMessage, MqttSessionImpl> {
    private static final int MAX_CLIENT_ID_LENGTH = 23;

    @Override
    public void process(BrokerContextImpl context, MqttSessionImpl session, MqttConnectMessage mqttConnectMessage) {
//        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        String clientId = mqttConnectMessage.getPayload().clientId();
        //服务端可以允许客户端提供一个零字节的客户端标识符 (ClientId) ，如果这样做了，服务端必须将这看作特
        //殊情况并分配唯一的客户端标识符给那个客户端。然后它必须假设客户端提供了那个唯一的客户端标识符，正常处理这个 CONNECT 报文
        if (clientId.isEmpty()) {
            clientId = MqttUtil.createClientId();
        }
        session.setClientId(clientId);
        //绑定MQTT Version
        session.setMqttVersion(mqttConnectMessage.getVersion());

        //有效性校验
        //服务端必须按照 3.1 节的要求验证 CONNECT 报文，如果报文不符合规范，服务端不发送CONNACK 报文直接关闭网络连接
        checkMessage(session, mqttConnectMessage);

        context.getEventBus().publish(EventType.CONNECT, EventObject.newEventObject(session, mqttConnectMessage));
        if (session.isDisconnect()) {
            return;
        }
        session.setAuthorized(true);
        //清理会话
        refreshSession(context, session, mqttConnectMessage);

        //存储遗嘱消息
        storeWillMessage(context, session, mqttConnectMessage);

        //存储连接属性
        session.setProperties(mqttConnectMessage.getVariableHeader().getProperties());
        int receiveMaximum;
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            //客户端使用此值限制客户端愿意同时处理的QoS等级1和QoS等级2的发布消息最大数量。
            receiveMaximum = Math.min(mqttConnectMessage.getVariableHeader().getProperties().getReceiveMaximum(), context.Options().getMaxInflight());
        } else {
            receiveMaximum = context.Options().getMaxInflight();
        }
        session.setInflightQueue(new InflightQueue(session, receiveMaximum, context.getInflightQueueTimer()));

        //如果服务端收到清理会话（CleanSession）标志为 1 的连接，除了将 CONNACK 报文中的返回码设置为 0 之外，
        // 还必须将 CONNACK 报文中的当前会话设置（Session Present）标志为 0。
        //如果服务端收到一个 CleanSession 为 0 的连接，当前会话标志的值取决于服务端是否已经保存了 ClientId 对应客户端的会话状态。
        // 如果服务端已经保存了会话状态，它必须将 CONNACK 报文中的当前会话标志设置为 1 。
        // 如果服务端没有已保存的会话状态，它必须将 CONNACK 报文中的当前会话设置为 0。还需要将 CONNACK 报文中的返回码设置为 0
        ConnectAckProperties properties = null;
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            properties = new ConnectAckProperties();
            properties.setReceiveMaximum(receiveMaximum);
        }
        MqttConnAckMessage mqttConnAckMessage = MqttSession.connAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, !mqttConnectMessage.getVariableHeader().isCleanSession(), properties);

        session.write(mqttConnAckMessage, false);
    }


    private void checkMessage(MqttSession session, MqttConnectMessage mqttConnectMessage) {
        MqttConnectVariableHeader connectVariableHeader = mqttConnectMessage.getVariableHeader();
        //如果协议名不正确服务端可以断开客户端的连接，也可以按照某些其它规范继续处理 CONNECT 报文。
        //对于后一种情况，按照本规范，服务端不能继续处理 CONNECT 报文。
        final MqttProtocolEnum protocol = MqttProtocolEnum.getByName(connectVariableHeader.protocolName());
        ValidateUtils.notNull(protocol, "invalid protocol", () -> {
            System.err.println("invalid protocol:" + connectVariableHeader.protocolName());
            //MQTT5.0规范：如果服务端不愿意接受CONNECT但希望表明其MQTT服务端身份，
            // 可以发送包含原因码为0x84（不支持的协议版本）的CONNACK报文，然后必须关闭网络连接。
            if (session.getMqttVersion() == MqttVersion.MQTT_5) {
                MqttSession.connFailAck(UNSUPPORTED_PROTOCOL_VERSION, session);
            }
            session.disconnect();
        });

        MqttConnectPayload payload = mqttConnectMessage.getPayload();
        String clientId = payload.clientId();

        //对于 3.1.1 版协议，协议级别字段的值是 4(0x04)。
        // 如果发现不支持的协议级别，服务端必须给发送一个返回码为 0x01（不支持的协议级别）的 CONNACK 报文响应
        //CONNECT 报文，然后断开客户端的连接
        final MqttVersion mqttVersion = MqttVersion.getByProtocolWithVersion(protocol, connectVariableHeader.getProtocolLevel());
        ValidateUtils.notNull(mqttVersion, "invalid version", () -> MqttSession.connFailAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, session));

        //服务端必须验证 CONNECT 控制报文的保留标志位（第 0 位）是否为 0，如果不为 0 必须断开客户端连接。
        ValidateUtils.isTrue(connectVariableHeader.getReserved() == 0, "", session::disconnect);

        //客户端标识符 (ClientId) 必须存在而且必须是 CONNECT 报文有效载荷的第一个字段
        //服务端必须允许 1 到 23 个字节长的 UTF-8 编码的客户端标识符，客户端标识符只能包含这些字符：
        //“0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ”（大写字母，小写字母和数字）
        boolean invalidClient = MqttUtil.isNotBlank(clientId) && (mqttVersion == MqttVersion.MQTT_3_1 && clientId.length() > MAX_CLIENT_ID_LENGTH);
        ValidateUtils.isTrue(!invalidClient, "", () -> {
            MqttSession.connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED, session);
        });
        //如果客户端提供的 ClientId 为零字节且清理会话标志为 0，
        // 服务端必须发送返回码为 0x02（表示标识符不合格）的 CONNACK 报文响应客户端的 CONNECT 报文，然后关闭网络连接
        ValidateUtils.isTrue(connectVariableHeader.isCleanSession() || !MqttUtil.isBlank(clientId), "", () -> {
            MqttSession.connFailAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED, session);
        });
    }

    private void refreshSession(BrokerContextImpl context, MqttSessionImpl session, MqttConnectMessage mqttConnectMessage) {
        session.setCleanSession(mqttConnectMessage.getVariableHeader().isCleanSession());
        MqttSessionImpl mqttSession = context.getSession(session.getClientId());
        if (mqttSession != null) {
            if (session.isCleanSession()) {
                //如果清理会话（CleanSession）标志被设置为 1，客户端和服务端必须丢弃之前的任何会话并开始一个新的会话。
                mqttSession.setCleanSession(true);
                mqttSession.disconnect();
            } else {
                //如果mqttSession#cleanSession为false，将还原会话状态
                mqttSession.disconnect();
                //如果清理会话（CleanSession）标志被设置为 0，服务端必须基于当前会话（使用客户端标识符识别）的状态恢复与客户端的通信。
                SessionStateProvider sessionStateProvider = context.getProviders().getSessionStateProvider();
                SessionState sessionState = sessionStateProvider.get(session.getClientId());
                if (sessionState != null) {
                    sessionState.getSubscribers().forEach(session::subscribe);
                    //客户端设置清理会话（CleanSession）标志为 0 重连时，客户端和服务端必须使用原始的报文标识符重发
                    //任何未确认的 PUBLISH 报文（如果 QoS>0）和 PUBREL 报文 [MQTT-4.4.0-1]。这是唯一要求客户端或
                    //服务端重发消息的情况。

                }
            }
        }

        context.addSession(session);
    }

    private void storeWillMessage(BrokerContext context, MqttSessionImpl session, MqttConnectMessage msg) {
        // 遗嘱标志（Will Flag）被设置为 1，表示如果连接请求被接受了，
        // 遗嘱（Will Message）消息必须被存储在服务端并且与这个网络连接关联。
        // 之后网络连接关闭时，服务端必须发布这个遗嘱消息，
        // 除非服务端收到 DISCONNECT 报文时删除了这个遗嘱消息
        if (!msg.getVariableHeader().isWillFlag()) {
            return;
        }
        WillMessage willMessage = msg.getPayload().getWillMessage();
        PublishBuilder publishBuilder = PublishBuilder.builder().topic(context.getOrCreateTopic(willMessage.getTopic())).qos(MqttQoS.valueOf(msg.getVariableHeader().willQos())).payload(willMessage.getPayload()).retained(msg.getFixedHeader().isRetain());
        //todo
        if (session.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }
        MqttPublishMessage publishMessage = publishBuilder.build();
        session.setWillMessage(publishMessage);
    }
}
