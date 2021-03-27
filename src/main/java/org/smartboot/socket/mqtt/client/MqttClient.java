package org.smartboot.socket.mqtt.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttProtocol;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.message.MqttConnectVariableHeader;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MqttClient implements Closeable {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private AioQuickClient client;
    private AioSession aioSession;
    private String host;
    private int port;
    private MqttConnectOptions connectOptions;

    public MqttClient() {
        this("localhost", 1883);
    }
    public MqttClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        connect(new MqttConnectOptions());
    }

    public void connect(MqttConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
        client = new AioQuickClient(host, port, new MqttProtocol(), new MqttClientProcessor());
        try {
            client.setReadBufferFactory(bufferPage -> VirtualBuffer.wrap(ByteBuffer.allocate(1024)));
            aioSession = client.start();
            WriteBuffer writeBuffer = aioSession.writeBuffer();
            MqttConnectMessage connectMessage = MqttMessageBuilders.connect().clientId("stw").keepAlive(60).cleanSession(true).build();
            connectMessage.writeTo(writeBuffer);
            TimeUnit.HOURS.sleep(1);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    @Override
    public void close() throws IOException {
        aioSession.close();
    }
}
