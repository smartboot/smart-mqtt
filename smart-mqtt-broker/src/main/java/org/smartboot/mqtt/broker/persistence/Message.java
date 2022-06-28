package org.smartboot.mqtt.broker.persistence;

import org.smartboot.mqtt.broker.eventbus.EventMessage;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public class Message extends ToString {
    /**
     * 消息等级
     */
    private final MqttQoS mqttQoS;
    /**
     * 负载数据
     */
    private final byte[] payload;
    /**
     * 主题
     */
    private final String topic;

    private final boolean retained;
    /**
     * 存储的消息偏移量
     */
    private final long offset;

    /**
     * 消息存储时间
     */
    private final long createTime = System.currentTimeMillis();

    public Message(EventMessage eventMessage, long offset) {
        this.mqttQoS = eventMessage.getMqttQoS();
        this.payload = eventMessage.getPayload();
        this.retained = eventMessage.isRetained();
        this.topic = eventMessage.getTopic();
        this.offset = offset;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public boolean isRetained() {
        return retained;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getOffset() {
        return offset;
    }
}
