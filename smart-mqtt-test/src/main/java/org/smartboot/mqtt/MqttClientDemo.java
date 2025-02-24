/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt;

import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.enums.MqttQoS;

public class MqttClientDemo {
    public static void main(String[] args) {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883");
        System.out.println(mqttClient.getClientId());
        mqttClient.getClientConfigure().setAutomaticReconnect(true);
        mqttClient.connect();
        String payload = "hello";
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    System.out.println("publish...");

                    try {
                        Thread.sleep(1000);
                        mqttClient.publish("a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
                    } catch (Exception e) {
//                      throw new RuntimeException(e);
                        e.printStackTrace();
                    }
                }
            }
        }).start();


        MqttClient mqttClient2 = new MqttClient("mqtt://127.0.0.1:1883");
        mqttClient2.getClientConfigure().setAutomaticReconnect(true);
        mqttClient2.connect();
        mqttClient2.subscribe("$share/a/a", MqttQoS.AT_MOST_ONCE, (m, mqttPublishMessage) -> {
            System.out.println("mqttClient2: " + new String(mqttPublishMessage.getPayload().getPayload()));
        });

        MqttClient mqttClient3 = new MqttClient("mqtt://127.0.0.1:1883");
        mqttClient3.getClientConfigure().setAutomaticReconnect(true);
        mqttClient3.connect();
        mqttClient3.subscribe("$share/a/a", MqttQoS.AT_MOST_ONCE, (m, mqttPublishMessage) -> {
            System.out.println("mqttClient3: " + new String(mqttPublishMessage.getPayload().getPayload()));
        });
    }
}
