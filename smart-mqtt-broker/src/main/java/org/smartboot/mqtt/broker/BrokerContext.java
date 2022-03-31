package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.enums.MqttQoS;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public interface BrokerContext {

    /**
     * 初始化Broker上下文
     */
    void init() throws IOException;

    BrokerConfigure getBrokerConfigure();

    MqttSession addSession(MqttSession session);

    boolean removeSession(MqttSession session);

    MqttSession getSession(String clientId);

    /**
     * 获取Topic，如果不存在将创建
     *
     * @param topic
     * @return
     */
    Topic getOrCreateTopic(String topic);

    /**
     * 发送消息至订阅者
     */
    void publish(Topic topic, MqttQoS mqttQoS, byte[] payload);

    ScheduledExecutorService getKeepAliveThreadPool();

    void destroy();
}
