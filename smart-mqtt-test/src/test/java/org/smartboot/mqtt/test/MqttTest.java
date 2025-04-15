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
import org.smartboot.mqtt.broker.BrokerContextImpl;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.payload.WillMessage;
import org.smartboot.mqtt.plugin.spec.BrokerContext;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MqttTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttTest.class);
    private BrokerContext brokerContext;

    @Before
    public void init() throws Throwable {
        System.setProperty("broker.maxPacketSize", String.valueOf(Integer.MAX_VALUE));
        System.setProperty("broker.bufferSize", String.valueOf(1024 * 1024));
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
    public void testA() throws Throwable {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883", opt -> opt.setMaxPacketSize(Integer.MAX_VALUE));
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        for (int i = 0; i <= 128; i++) {
            checkPayloadSize(i, mqttClient);
        }
        for (int i = 128; i <= 256; i++) {
            checkPayloadSize(i, mqttClient);
        }

        for (int i = 16300; i <= 16383; i++) {
            checkPayloadSize(i, mqttClient);
        }

        for (int i = 16384; i <= 16400; i++) {
            checkPayloadSize(i, mqttClient);
        }
        for (int i = 2097100; i <= 2097151; i++) {
            checkPayloadSize(i, mqttClient);
        }
        for (int i = 2097152; i <= 2097160; i++) {
            checkPayloadSize(i, mqttClient);
        }
        mqttClient.disconnect();
    }

    @Test
    public void testB() throws Throwable {
        System.setProperty("broker.maxPacketSize", String.valueOf(Integer.MAX_VALUE));
        System.setProperty("broker.bufferSize", String.valueOf(16 * 1024 * 1024));
        brokerContext.destroy();
        brokerContext = new BrokerContextImpl();
        brokerContext.init();
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883", opt -> opt.setMaxPacketSize(Integer.MAX_VALUE).setBufferSize(1024 * 1024 * 16));
        System.out.println(mqttClient.getClientId());
        mqttClient.connect();
        for (int i = 268435441; i <= 268435451; i++) {
            checkPayloadSize(i, mqttClient);
            System.out.println("index:" + i);
        }
        mqttClient.disconnect();
    }

    private static void checkPayloadSize(int i, MqttClient mqttClient) throws InterruptedException, ExecutionException {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        byte[] bytes = new byte[i];
        Arrays.fill(bytes, (byte) i);
        mqttClient.subscribe("/a", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
            completableFuture.complete(mqttPublishMessage.getPayload().getPayload());
        });
        mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, bytes);
        Assert.assertArrayEquals("index: " + i, bytes, completableFuture.get());
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
            countDownLatch.countDown();
        }, (mqttClient1, mqttQoS) -> {
            int j = i;
            while (j-- > 0) {
                mqttClient.publish("/a", MqttQoS.AT_MOST_ONCE, payload.getBytes());
            }
        });
        countDownLatch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
        System.out.println("count: " + countDownLatch.getCount());
    }

    @Test
    public void testWillMessage() throws InterruptedException, ExecutionException {
        MqttClient mqttClient = new MqttClient("mqtt://127.0.0.1:1883", opt -> {
            WillMessage willMessage = new WillMessage();
            willMessage.setTopic("/will");
            willMessage.setWillQos(MqttQoS.AT_MOST_ONCE);
            willMessage.setPayload("willPayload".getBytes());
            opt.setWillMessage(willMessage);
        });

        mqttClient.connect();

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        MqttClient mqttClient2 = new MqttClient("mqtt://127.0.0.1:1883");
        mqttClient2.connect();
        mqttClient2.subscribe("/will", MqttQoS.AT_MOST_ONCE, (mqttClient1, mqttPublishMessage) -> {
            System.out.println(new String(mqttPublishMessage.getPayload().getPayload()));
            completableFuture.complete(new String(mqttPublishMessage.getPayload().getPayload()));
        }, (mqttClient1, mqttQoS) -> mqttClient.disconnect());

        Assert.assertEquals(completableFuture.get(), "willPayload");
    }
}
