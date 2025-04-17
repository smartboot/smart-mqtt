/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.bench;

import org.apache.commons.lang.math.NumberUtils;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttQoS;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/10
 */
public class Publish extends AbstractBench {
    public static void main(String[] args) throws IOException {
//        System.setProperty("host", "127.0.0.1");
//        System.setProperty("count", "10");
//        System.setProperty("connect", "2000");
        int size = NumberUtils.toInt(System.getProperty("payload"), 1024);
        int topic = NumberUtils.toInt(System.getProperty("topic"), 128);
        int publishCount = NumberUtils.toInt(System.getProperty("count"), 1);
        int qos = NumberUtils.toInt(System.getProperty("qos"), 0);
        int period=NumberUtils.toInt(System.getProperty("period"), 1);
        if(period<1){
            period=1;
        }

        Publish connect = new Publish();
        AtomicInteger i = new AtomicInteger();
        byte[] payload = new byte[size];
        Arrays.fill(payload, (byte) -1);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        List<MqttClient> clients = new CopyOnWriteArrayList<>();
        connect.bench(clients::add);

        for (MqttClient mqttClient : clients) {
            executorService.scheduleWithFixedDelay(() -> {
                for (int j = 0; j < publishCount; j++) {
                    try {
                        mqttClient.publish("/topic" + (i.incrementAndGet() % topic), MqttQoS.valueOf(qos), payload, false, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) {
                                connect.countAdder.increment();
                            }
                        }, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                mqttClient.flush();

            }, period, period, TimeUnit.MILLISECONDS);
        }
    }

}
