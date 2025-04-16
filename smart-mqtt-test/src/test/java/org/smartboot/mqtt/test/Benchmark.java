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

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/18
 */
public class Benchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);
    private final String host = "127.0.0.1";
    private final int port = 1883;
    private AsynchronousChannelGroup channelGroup;

    private BrokerContextImpl context;

    @Before
    public void init() throws Throwable {
        channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, r -> new Thread(r));
        context = new BrokerContextImpl();
        context.init();
    }

    @After
    public void destroy() {
        channelGroup.shutdown();
        context.destroy();
    }

    /**
     * 建立连接后再断开连接
     */
    @Test
    public void testConnect() throws InterruptedException {
        int connectCount = 10000;
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        long s = System.currentTimeMillis();
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(channelGroup));
            long start = System.currentTimeMillis();
            client.connect(connAckMessage -> {
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
                client.disconnect();
            });
        }
        countDownLatch.await();
        System.out.println((System.currentTimeMillis() - s) + ":" + total.get());
    }


    /**
     * 订阅topic成功后断开连接
     *
     * @throws InterruptedException
     */
    @Test
    public void testSubscribe() throws InterruptedException {
        int connectCount = 10000;
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        long s = System.currentTimeMillis();
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(channelGroup));
            long start = System.currentTimeMillis();
            client.subscribe("/topic/" + connectCount, MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {

            }, (mqttClient, mqttQoS) -> {
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
                client.disconnect();
            });
            client.connect();
        }
        countDownLatch.await();
        System.out.println((System.currentTimeMillis() - s) + ":" + total.get());
    }

    @Test(timeout = 3000)
    public void testSubscribe2() throws InterruptedException {
        MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(channelGroup));
        client.connect();

        int connectCount = 10000;
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        long s = System.currentTimeMillis();

        while (connectCount-- > 0) {
            long start = System.currentTimeMillis();
            String topic = "/topic/" + connectCount;
            client.subscribe(topic, MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {
            }, (mqttClient, mqttQoS) -> {
//                LOGGER.info("subscribe:{}", "/topic/" + topic);
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
            });

        }
        countDownLatch.await();
        client.disconnect();
        System.out.println((System.currentTimeMillis() - s) + ":" + total.get());
    }

    /**
     * 订阅topic成功后断开连接
     *
     * @throws InterruptedException
     */
    @Test(timeout = 5000)
    public void testPublish() throws InterruptedException {
        int connectCount = 100;
        int publishCount = Short.MAX_VALUE;

        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        CountDownLatch publishDownLatch = new CountDownLatch(connectCount * publishCount);
        System.out.println("publish count: " + publishDownLatch.getCount());
        long s = System.currentTimeMillis();
        String topic = "/topic";
        //建立 1W 个订阅
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(channelGroup));
            client.subscribe(topic, MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {
//                System.out.println(publishDownLatch.getCount());
                publishDownLatch.countDown();
            }, (mqttClient, mqttQoS) -> countDownLatch.countDown());
            client.connect();
        }
        countDownLatch.await();
        MqttClient client = new MqttClient(host, port);
        client.connect();

        System.out.println("start publish message");
        long startPublish = System.currentTimeMillis();
        while (publishCount-- > 0) {
            client.publish(topic, MqttQoS.AT_MOST_ONCE, payload, false);
        }
        System.out.println("publish finish!");
        Assert.assertTrue("wait result timeout", publishDownLatch.await(5, TimeUnit.SECONDS));
        System.out.println("publish use time: " + (System.currentTimeMillis() - startPublish) + " count:" + publishDownLatch.getCount());
    }
}
