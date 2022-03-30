package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClientSession;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnAckVariableHeader;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttFixedHeader;

import java.nio.ByteBuffer;

/**
 * 连接处理器
 *
 * @author stw
 * @version V1.0 , 2018/4/25
 */
public class ConnAckProcessor implements MqttProcessor<MqttConnAckMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnAckProcessor.class);

    private boolean allowZeroByteClientId = false;

    private boolean allowAnonymous = false;

    @Override
    public void process(MqttClientSession session, MqttConnAckMessage mqttConnAckMessage) {
        LOGGER.info("receive connectAck message:{}", mqttConnAckMessage);
        MqttConnectReturnCode returnCode = mqttConnAckMessage.getMqttConnAckVariableHeader().connectReturnCode();
        switch (returnCode) {
            case CONNECTION_ACCEPTED:
                LOGGER.info("连接建立");
                break;
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
                LOGGER.error("未授权");
                break;
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                LOGGER.error("username或password错误");
                break;
            default:
                break;
        }
//        MqttConnAckMessage mqttConnAckMessage = MqttMessageBuilders.connAck()
//                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
//                .sessionPresent(true).build();
//        session.write(mqttConnAckMessage);
//        LOGGER.info("response connect message:{}", mqttConnAckMessage);
//
//
//        MqttConnectPayload payload = mqttConnAckMessage.getPayload();
//        String clientId = payload.clientIdentifier();
//        LOGGER.info("Processing CONNECT message. CId={}, username={}", clientId, payload.userName());
//
//        if (mqttConnAckMessage.getVariableHeader().version() != MqttVersion.MQTT_3_1.protocolLevel()
//                && mqttConnAckMessage.getVariableHeader().version() != MqttVersion.MQTT_3_1_1.protocolLevel()) {
//            MqttConnAckMessage badProto = connAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false);
//
//            LOGGER.error("MQTT protocol version is not valid. CId={}", clientId);
//            session.write(badProto);
//            session.close();
//            return;
//        }
//
//        final boolean cleanSession = mqttConnAckMessage.getVariableHeader().isCleanSession();
//        if (clientId == null || clientId.length() == 0) {
//            if (!cleanSession || !this.allowZeroByteClientId) {
//                MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED, false);
//
//                session.write(badId);
//                session.close();
//                LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
//                return;
//            }
//
//            // Generating client id.
//            clientId = UUID.randomUUID().toString().replace("-", "");
//            LOGGER.info("Client has connected with a server generated identifier. CId={}, username={}", clientId,
//                    payload.userName());
//        }
//
//        if (!login(session, mqttConnAckMessage, clientId)) {
//            session.close();
//            return;
//        }
//
//        session.init(clientId, cleanSession);
//        MqttSession existing = context.addSession(session);
//        if (existing != null) {
//            LOGGER.info("Client ID is being used in an existing connection, force to be closed. CId={}", clientId);
//            existing.close();
//            //return;
//            context.removeSession(existing);
//            context.addSession(session);
//        }
//
////        initializeKeepAliveTimeout(channel, msg, clientId);
//        storeWillMessage(mqttConnAckMessage, clientId);
//
//
//        LOGGER.info("CONNECT message processed CId={}, username={}", clientId, payload.userName());
    }


    private void storeWillMessage(MqttConnectMessage msg, final String clientId) {
        // Handle will flag
        if (msg.getVariableHeader().isWillFlag()) {
            MqttQoS willQos = MqttQoS.valueOf(msg.getVariableHeader().willQos());
            LOGGER.info("Configuring MQTT last will and testament CId={}, willQos={}, willTopic={}, willRetain={}", clientId, willQos, msg.getPayload().willTopic(), msg.getVariableHeader().isWillRetain());
            byte[] willPayload = msg.getPayload().willMessage().getBytes();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            // save the will testament in the clientID store
//            WillMessage will = new WillMessage(msg.payload().willTopic(), bb, msg.variableHeader().isWillRetain(),
//                    willQos);
//            m_willStore.put(clientId, will);
            LOGGER.info("MQTT last will and testament has been configured. CId={}", clientId);
        }
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }
}
