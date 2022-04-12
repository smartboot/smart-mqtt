package org.smartboot.mqtt.client;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.WillMessage;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.nio.charset.StandardCharsets;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientBootstrap {

    public static void main(String[] args) {
        MqttClient client = new MqttClient("localhost", 1883, MqttUtil.createClientId());
        //心跳
        client.getClientConfigure().setKeepAliveInterval(2).setAutomaticReconnect(true);

        //遗嘱消息
        WillMessage willMessage = new WillMessage();
        willMessage.setWillTopic("willTopic");
        willMessage.setWillRetain(true);
        willMessage.setWillMessage("helloWorld".getBytes(StandardCharsets.UTF_8));
        willMessage.setWillQos(MqttQoS.AT_MOST_ONCE);
        client.willMessage(willMessage);

        //连接broker
        client.connect();

        //订阅主题
        client.subscribe("test", MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {
            System.out.println("subscribe message:" + new String(publishMessage.getPayload()));
        });

        //最多分发一次
        client.publish("test", MqttQoS.AT_MOST_ONCE, "aa".getBytes(StandardCharsets.UTF_8), false, packetId -> System.out.println("发送结果：" + packetId));
        //至少分发一次
        client.publish("test", MqttQoS.AT_LEAST_ONCE, "bb".getBytes(StandardCharsets.UTF_8), false, packetId -> System.out.println("发送结果：" + packetId));
        //只分发一次
        client.publish("test", MqttQoS.EXACTLY_ONCE, "cc".getBytes(StandardCharsets.UTF_8), false, packetId -> System.out.println("发送结果：" + packetId));
    }
}
