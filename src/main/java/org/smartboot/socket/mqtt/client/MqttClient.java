package org.smartboot.socket.mqtt.client;

import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.mqtt.MqttProtocol;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class MqttClient implements Closeable {

    private AioQuickClient client;
    private AioSession aioSession;
    private String host;
    private int port;
    private MqttConnectOptions connectOptions;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    @Override
    public void close() throws IOException {

    }
}
