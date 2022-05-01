package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.enums.MqttQoS;

import java.util.concurrent.Semaphore;

/**
 * Topic订阅者
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class TopicSubscriber {
    private final MqttSession mqttSession;
    /**
     * 定义消息主题
     */
    private final BrokerTopic topic;
    /**
     * 服务端向客户端发送应用消息所允许的最大 QoS 等级
     */
    private final MqttQoS mqttQoS;
    /**
     * 是否可用
     */
    private boolean enable = true;

    /**
     * 期望消费的点位
     */
    private long nextConsumerOffset;

    /**
     * retain的消费点位，防止重连后的retain消息被重复消费
     */
    private long retainConsumerOffset;

    private final Semaphore semaphore = new Semaphore(1);

    public TopicSubscriber(BrokerTopic topic, MqttSession session, MqttQoS mqttQoS, long nextConsumerOffset, long retainConsumerOffset) {
        this.topic = topic;
        this.mqttSession = session;
        this.mqttQoS = mqttQoS;
        this.nextConsumerOffset = nextConsumerOffset;
        this.retainConsumerOffset = retainConsumerOffset;
    }

    public BrokerTopic getTopic() {
        return topic;
    }

    public MqttSession getMqttSession() {
        return mqttSession;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public long getNextConsumerOffset() {
        return nextConsumerOffset;
    }

    public void setNextConsumerOffset(long nextConsumerOffset) {
        this.nextConsumerOffset = nextConsumerOffset;
        //retain点位保持同步，防止断链重连后消息被重复消费
        this.retainConsumerOffset = nextConsumerOffset;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public long getRetainConsumerOffset() {
        return retainConsumerOffset;
    }

    public void setRetainConsumerOffset(long retainConsumerOffset) {
        this.retainConsumerOffset = retainConsumerOffset;
    }
}
