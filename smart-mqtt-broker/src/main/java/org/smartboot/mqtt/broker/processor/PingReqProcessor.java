package org.smartboot.mqtt.broker.processor;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttPingReqMessage;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;

/**
 * 心跳请求处理
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PingReqProcessor extends AuthorizedMqttProcessor<MqttPingReqMessage> {
    private static final MqttPingRespMessage RESP_MESSAGE = new MqttPingRespMessage();

    @Override
    public void process0(BrokerContext context, MqttSession session, MqttPingReqMessage msg) {
        session.write(RESP_MESSAGE);
    }
}
