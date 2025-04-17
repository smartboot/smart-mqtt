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
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import tech.smartboot.mqtt.client.MqttClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/10
 */
public class AbstractBench {
    public final LongAdder countAdder = new LongAdder();

    protected void bench(Consumer<MqttClient> consumer) throws IOException {
        //emqx启动较慢
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int count = NumberUtils.toInt(System.getProperty("connect"), 1000);
        boolean lowMemory = Boolean.parseBoolean(System.getenv("BROKER_LOWMEMORY"));
        System.out.println("lowMemory: " + lowMemory);
        AsynchronousChannelGroup channelGroup = new EnhanceAsynchronousChannelProvider(lowMemory).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "client-pool");
            }
        });
        while (count-- > 0) {
            MqttClient client = newClient(channelGroup);
            client.connect(mqttConnAckMessage -> consumer.accept(client));
        }
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            int c = countAdder.intValue();
            countAdder.add(-c);
            System.out.println("total: " + c + "\tTPS: " + (c / 5));
        }, 5, 5, TimeUnit.SECONDS);
    }

    public MqttClient newClient(AsynchronousChannelGroup channelGroup) {
        String host = System.getProperty("host");
        int port = NumberUtils.toInt(System.getProperty("port"), 1883);
        MqttClient client = new MqttClient(host, port, opt -> opt.setGroup(channelGroup).setKeepAliveInterval(30).setAutomaticReconnect(true));
        return client;
    }
}
