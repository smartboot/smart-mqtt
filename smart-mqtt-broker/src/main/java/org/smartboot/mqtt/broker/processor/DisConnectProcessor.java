package org.smartboot.mqtt.broker.processor;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttDisconnectMessage;

/**
 * DISCONNECT 报文是客户端发给服务端的最后一个控制报文。表示客户端正常断开连接。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/27
 */
public class DisConnectProcessor extends AuthorizedMqttProcessor<MqttDisconnectMessage> {
    @Override
    public void process0(BrokerContext context, MqttSession session, MqttDisconnectMessage message) {
        session.setWillMessage(null);
        session.close();
    }
}
