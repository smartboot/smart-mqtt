package tech.smartboot.mqtt.plugin.cluster;

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
}
