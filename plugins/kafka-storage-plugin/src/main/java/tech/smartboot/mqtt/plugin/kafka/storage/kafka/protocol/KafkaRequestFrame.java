package tech.smartboot.mqtt.plugin.kafka.storage.kafka.protocol;

import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;

import java.nio.ByteBuffer;

public class KafkaRequestFrame {
    private final ApiKeys apiKey;
    private final short apiVersion;
    private final short headerVersion;
    private final RequestHeaderData header;
    private final ByteBuffer body;

    public KafkaRequestFrame(ApiKeys apiKey,
                             short apiVersion,
                             short headerVersion,
                             RequestHeaderData header,
                             ByteBuffer body) {
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
        this.headerVersion = headerVersion;
        this.header = header;
        this.body = body;
    }

    public ApiKeys getApiKey() {
        return apiKey;
    }

    public short getApiVersion() {
        return apiVersion;
    }

    public short getHeaderVersion() {
        return headerVersion;
    }

    public RequestHeaderData getHeader() {
        return header;
    }

    public ByteBuffer getBody() {
        return body.duplicate();
    }
}
