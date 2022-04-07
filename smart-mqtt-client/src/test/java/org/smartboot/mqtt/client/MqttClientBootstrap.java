package org.smartboot.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.WillMessage;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttClientBootstrap.class);

    public static void main(String[] args) {
        MqttClient client = new MqttClient("localhost", 1883, "stw");
        client.getClientConfigure().setKeepAliveInterval(2);
        WillMessage willMessage = new WillMessage();
        willMessage.setWillTopic("will");
        willMessage.setWillRetain(true);
        willMessage.setWillMessage("a".getBytes(StandardCharsets.UTF_8));
        willMessage.setWillQos(MqttQoS.AT_MOST_ONCE);
        client.willMessage(willMessage)
                .connect();

        client.subscribe("test", MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {
            System.out.println("aaaa");
        });

        client.publish("test", MqttQoS.AT_MOST_ONCE, "aa".getBytes(StandardCharsets.UTF_8), false, new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println("发送结果：" + integer);
            }
        });
//        while (true){
//            client.pub("test", MqttQoS.EXACTLY_ONCE, "test".getBytes());
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
