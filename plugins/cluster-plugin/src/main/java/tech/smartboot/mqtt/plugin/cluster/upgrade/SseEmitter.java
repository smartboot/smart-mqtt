package tech.smartboot.mqtt.plugin.cluster.upgrade;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.mqtt.plugin.cluster.BinaryServerSentEventStream;

import java.io.IOException;
import java.util.function.Consumer;

public class SseEmitter {
    private String accessToken;
    private final AioSession aioSession;
    private static final byte[] topic = new byte[]{BinaryServerSentEventStream.TAG_TOPIC, ':'};
    private static final byte[] retain = new byte[]{'\n', BinaryServerSentEventStream.TAG_RETAIN, ':'};
    private static final byte[] payload = new byte[]{'\n', BinaryServerSentEventStream.TAG_RETAIN, ':'};

    public SseEmitter(AioSession aioSession) {
        this.aioSession = aioSession;
    }

    public void send(byte[] bytes) {
        WriteBuffer buffer = aioSession.writeBuffer();
        try {
            buffer.write(bytes);
            aioSession.writeBuffer().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void onTimeout(Runnable callback) {
    }

    public synchronized void onError(Consumer<Throwable> callback) {
    }

    public synchronized void onCompletion(Runnable callback) {
    }

    public void complete() {
        aioSession.close();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
