package tech.smartboot.mqtt.plugin.cluster;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public class MqttMessage {
    /**
     * 负载数据
     */
    private byte[] payload;

    /**
     * 主题
     */
    private String topic;

    private boolean retained;


    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public byte[] toBytes() {
        ByteOutputStream byteOutputStream = new ByteOutputStream(payload.length + 32);
        byteOutputStream.write(BinaryServerSentEventStream.TAG_TOPIC);
        byteOutputStream.write(':');
        byteOutputStream.write(topic.getBytes());
        byteOutputStream.write('\n');

        if (retained) {
            byteOutputStream.write(BinaryServerSentEventStream.TAG_RETAIN);
            byteOutputStream.write(':');
            byteOutputStream.write('\n');
        }
        byteOutputStream.write(BinaryServerSentEventStream.TAG_PAYLOAD);
        byteOutputStream.write(':');
        byteOutputStream.write((payload.length + " ").getBytes());
        byteOutputStream.write(payload);
        byteOutputStream.write('\n');
        byteOutputStream.write('\n');
        return byteOutputStream.toByteArray();
    }
}
