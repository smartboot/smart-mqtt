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
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/10
 */
public class Subscribe extends AbstractBench {
    public static void main(String[] args) throws IOException, InterruptedException {
//        System.setProperty("host", "127.0.0.1");
//        System.setProperty("count", "10");
//        System.setProperty("connect", "2000");
//        System.setProperty("qos", "0");
//        System.setProperty("publisher", "1");
        int size = NumberUtils.toInt(System.getProperty("payload"), 128);
        int topic = NumberUtils.toInt(System.getProperty("topic"), 128);
        int publisher = NumberUtils.toInt(System.getProperty("publisher"), 1);
        int publishCount = NumberUtils.toInt(System.getProperty("count"), 2);
        int qos = NumberUtils.toInt(System.getProperty("qos"), 0);
        int period = NumberUtils.toInt(System.getProperty("period"), 1);
        if (period < 1) {
            period = 1;
        }
        long random = System.currentTimeMillis() % 10000;
        Subscribe subscribe = new Subscribe();

        CountDownLatch countDownLatch = new CountDownLatch(NumberUtils.toInt(System.getProperty("connect")) * topic);
        subscribe.bench(mqttClient -> {
            for (int j = 0; j < topic; j++) {
                mqttClient.subscribe("topic_" + random + "_" + j, MqttQoS.valueOf(qos), (mqttClient1, mqttPublishMessage) -> {
//                System.out.println("receive...");
                    subscribe.countAdder.increment();
                }, (mqttClient12, mqttQoS) -> countDownLatch.countDown());
            }
        });
        countDownLatch.await();
        System.out.println("订阅完毕");
        if (publisher == 0) {
            return;
        }
        AtomicInteger index = new AtomicInteger();
        byte[] payload = new byte[size];
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        AsynchronousChannelGroup channelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "client" +
                "-pool"));
        for (int i = 0; i < publisher; i++) {
            MqttClient publish = subscribe.newClient(channelGroup);
            publish.connect();
            executorService.scheduleWithFixedDelay(() -> {
                for (int j = 0; j < publishCount; j++) {
                    publish.publish("topic_" + random + "_" + (index.incrementAndGet() % topic), MqttQoS.AT_MOST_ONCE, payload, false, false);
                }
                publish.flush();
            }, period, period, TimeUnit.MILLISECONDS);
        }
    }

}
