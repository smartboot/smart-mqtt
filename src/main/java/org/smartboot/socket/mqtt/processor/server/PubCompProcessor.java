package org.smartboot.socket.mqtt.processor.server;

import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttPubRelMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.push.QosTask;

/**
 * PUBCOMP 报文是对 PUBREL 报文的响应。它是 QoS 2 等级协议交换的第四个也是最后一个报文。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/27
 */
public class PubCompProcessor implements MqttProcessor<MqttPubRelMessage> {
    @Override
    public void process(MqttContext context, MqttSession session, MqttPubRelMessage mqttPubRelMessage) {
        QosTask task = session.getQosTask(mqttPubRelMessage.getPacketId());
        task.done();
    }
}
