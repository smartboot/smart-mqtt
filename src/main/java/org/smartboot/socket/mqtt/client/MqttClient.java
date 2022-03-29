package org.smartboot.socket.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttProtocol;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.*;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class MqttClient implements Closeable {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final int MAX_PACKET_ID = 65535;

    private AioQuickClient client;
    private AioSession aioSession;
    private final String host;
    private final int port;
    private final String clientId;
    private MqttConnectOptions connectOptions;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private BufferPagePool bufferPagePool;
    private LongAdder longAdder = new LongAdder();
    public MqttCallback callback;

    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public MqttClient() {
        this("localhost", 1883, UUID.randomUUID().toString());
    }
    public MqttClient(String host, int port, String clientId) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
    }

    public void connect() {
        try {
            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                private AtomicInteger index = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "ClientGroup" + index.getAndIncrement());
                }
            });
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        connect(new MqttConnectOptions(),asynchronousChannelGroup);
    }

    public void reconnect() {
        connect(new MqttConnectOptions(), asynchronousChannelGroup);
    }

    public void connect(MqttConnectOptions connectOptions, AsynchronousChannelGroup asynchronousChannelGroup) {
        this.connectOptions = connectOptions;
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "org.smartboot.aio.EnhanceAsynchronousChannelProvider");
        if (bufferPagePool == null) {
            bufferPagePool = new BufferPagePool(1024 * 1024 * 2, 10, true);
        }
        client = new AioQuickClient(host, port, new MqttProtocol(), new MqttClientProcessor(this));
        try {
            client.setBufferPagePool(bufferPagePool);
            client.setWriteBuffer(1024 * 1024, 10);
            aioSession = client.start(asynchronousChannelGroup);
            WriteBuffer writeBuffer = aioSession.writeBuffer();
            MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
                    .clientId(clientId).keepAlive(connectOptions.getKeepAliveInterval()).cleanSession(connectOptions.isCleanSession()).build();
            connectMessage.writeTo(writeBuffer);

            startPing(connectOptions, writeBuffer);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void startPing(MqttConnectOptions connectOptions, WriteBuffer writeBuffer) {
        executorService.scheduleAtFixedRate(()->{
            MqttPingReqMessage pingReqMessage = MqttMessageBuilders.pingReq().build();
            synchronized (writeBuffer){
                pingReqMessage.writeTo(writeBuffer);
            }
        },3, connectOptions.getKeepAliveInterval(),TimeUnit.SECONDS);
    }

    public void stopPing() {
        executorService.shutdown();
    }

    public void sub(String topic, MqttQoS qos){
        int packetId = getPacketId();
        MqttSubscribeMessage subMsg = MqttMessageBuilders.subscribe().packetId(packetId).addSubscription(qos, topic).build();
        synchronized (aioSession.writeBuffer()){
            try {
                subMsg.writeTo(aioSession.writeBuffer());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void pub(String topic, MqttQoS qos, byte[] payload){
        MqttPublishMessage publishMessage;
        if (qos.equals(MqttQoS.AT_MOST_ONCE)){
            publishMessage = MqttMessageBuilders.publish().topicName(topic).qos(qos).payload(payload).build();
        }else {
            int packetId = getPacketId();
            publishMessage = MqttMessageBuilders.publish().packetId(packetId).topicName(topic).qos(qos).payload(payload).build();
        }
        synchronized (aioSession.writeBuffer()){
            try {
                publishMessage.writeTo(aioSession.writeBuffer());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void callBack(MqttCallback callback){
        this.callback = callback;
    }

    private int getPacketId() {
        longAdder.increment();
        int packetId = longAdder.intValue();
        if (packetId > MAX_PACKET_ID){
            packetId = 1;
            longAdder.reset();
            longAdder.increment();
        }
        return packetId;
    }

    @Override
    public void close() throws IOException {
        aioSession.close();
    }


}
