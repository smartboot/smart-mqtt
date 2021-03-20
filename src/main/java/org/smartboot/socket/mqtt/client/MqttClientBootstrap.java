package org.smartboot.socket.mqtt.client;

import org.smartboot.socket.mqtt.MqttProtocol;
import org.smartboot.socket.mqtt.client.MqttClientProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttClientBootstrap {
    public static void main(String[] args) {
        AioQuickClient client = new AioQuickClient("localhost",1883,new MqttProtocol(),new MqttClientProcessor());
        try {
            AioSession session = client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
