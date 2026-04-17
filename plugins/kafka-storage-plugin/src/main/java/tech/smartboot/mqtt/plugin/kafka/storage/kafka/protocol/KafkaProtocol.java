package tech.smartboot.mqtt.plugin.kafka.storage.kafka.protocol;

import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class KafkaProtocol implements Protocol<KafkaRequestFrame> {
    private final int maxRequestBytes;

    public KafkaProtocol(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    public KafkaRequestFrame decode(ByteBuffer readBuffer, AioSession session) {
        if (readBuffer.remaining() < 4) {
            return null;
        }
        readBuffer.mark();
        int frameLength = readBuffer.getInt();
        if (frameLength <= 0 || frameLength > maxRequestBytes) {
            throw new IllegalStateException("illegal kafka frame length: " + frameLength);
        }
        if (readBuffer.remaining() < frameLength) {
            readBuffer.reset();
            return null;
        }
        ByteBuffer frameBuffer = readBuffer.slice();
        frameBuffer.limit(frameLength);
        readBuffer.position(readBuffer.position() + frameLength);

        ByteBuffer headerProbe = frameBuffer.duplicate();
        short apiKeyId = headerProbe.getShort();
        short apiVersion = headerProbe.getShort();
        ApiKeys apiKey = ApiKeys.forId(apiKeyId);
        short headerVersion = apiKey.requestHeaderVersion(apiVersion);

        ByteBufferAccessor accessor = new ByteBufferAccessor(frameBuffer);
        RequestHeaderData header = new RequestHeaderData(accessor, headerVersion);
        ByteBuffer body = frameBuffer.slice();
        return new KafkaRequestFrame(apiKey, apiVersion, headerVersion, header, body);
    }
}
