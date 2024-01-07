/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerContextImpl;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MqttTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttTest.class);
    private BrokerContext brokerContext;

    @Before
    public void init() throws Throwable {
        brokerContext = new BrokerContextImpl();
        brokerContext.init();
    }

    @After
    public void destroy() {
        brokerContext.destroy();
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883");
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        String payload = "hello";
        mqttClient.subscribe("/a", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
            completableFuture.complete(new String(mqttPublishMessage.getPayload().getPayload()));
        });
        mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
        Assert.assertEquals(payload, completableFuture.get());
        LOGGER.info("payload: {}", completableFuture.get());
        mqttClient.disconnect();
    }

    @Test
    public void test1() throws ExecutionException, InterruptedException {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883");
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        String payload = "hello";
        mqttClient.subscribe("/a", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
            completableFuture.complete(new String(mqttPublishMessage.getPayload().getPayload()));
        });
        mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
        Thread.sleep(100);
        mqttClient.disconnect();
        Assert.assertEquals(payload, completableFuture.get());
        LOGGER.info("payload: {}", completableFuture.get());
    }

    @Test
    public void test2() throws InterruptedException {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883");
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        final int i = 100;
        CountDownLatch countDownLatch = new CountDownLatch(i);
        String payload = "hello";
        mqttClient.subscribe("/a", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
//            System.out.println(new String(mqttPublishMessage.getPayload().getPayload()));
            countDownLatch.countDown();
        }, (mqttClient1, mqttQoS) -> {
            int j = i;
            while (j-- > 0) {
                mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
            }
        });
        countDownLatch.await(1, TimeUnit.SECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
    }

    @Test
    public void test3() throws InterruptedException {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883");
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        final int i = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(i);
        String payload = "hello";
        mqttClient.subscribe("/a", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
//            System.out.println(new String(mqttPublishMessage.getPayload().getPayload()));
            countDownLatch.countDown();
        }, (mqttClient1, mqttQoS) -> {
            int j = i;
            while (j-- > 0) {
                mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
            }
        });
        countDownLatch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
        System.out.println("count: " + countDownLatch.getCount());
    }
}
