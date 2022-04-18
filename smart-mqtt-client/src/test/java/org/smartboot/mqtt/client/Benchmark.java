package org.smartboot.mqtt.client;

import org.junit.Test;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/18
 */
public class Benchmark {
    private String host = "127.0.0.1";
    private int port = 1883;

    /**
     * 建立连接后再断开连接
     */
    @Test
    public void testConnect() throws IOException, InterruptedException {
        int connectCount = 10000;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, MqttUtil.createClientId());
            long start = System.currentTimeMillis();
            client.connect(channelGroup, connAckMessage -> {
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
                client.close();
            });
        }
        countDownLatch.await();
        System.out.println(total.get());
        channelGroup.shutdown();
    }


    /**
     * 订阅topic成功后断开连接
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testSubscribe() throws IOException, InterruptedException {
        int connectCount = 10000;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, MqttUtil.createClientId());
            long start = System.currentTimeMillis();
            client.subscribe("/topic/" + connectCount, MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {

            }, (mqttClient, mqttQoS) -> {
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
                client.close();
            });
            client.connect(channelGroup);
        }
        countDownLatch.await();
        System.out.println(total.get());
        channelGroup.shutdown();
    }

    /**
     * 订阅topic成功后断开连接
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testPublish() throws IOException, InterruptedException {
        int connectCount = 10000;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        AtomicLong total = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(connectCount);
        while (connectCount-- > 0) {
            MqttClient client = new MqttClient(host, port, MqttUtil.createClientId());
            long start = System.currentTimeMillis();
            String topic = "/topic/" + connectCount;
            client.subscribe(topic, MqttQoS.AT_MOST_ONCE, (mqttClient, publishMessage) -> {
                total.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
                client.close();
            }, (mqttClient, mqttQoS) -> {
                client.publish(topic, MqttQoS.AT_MOST_ONCE, payload, false);
            });
            client.connect(channelGroup);
        }
        countDownLatch.await();
        System.out.println(total.get());
        channelGroup.shutdown();
    }
}
