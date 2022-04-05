package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.plugin.provider.Providers;
import org.smartboot.mqtt.common.StoredMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.EventListener;
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
    BrokerTopic getOrCreateTopic(String topic);

    /**
     * 获得当前的Topic列表
     */
    Collection<BrokerTopic> getTopics();


    /**
     * 发送消息至订阅者
     */
    void publish(BrokerTopic topic, StoredMessage storedMessage);

    ScheduledExecutorService getKeepAliveThreadPool();

    void destroy();

    Providers getProviders();

    void addEvent(EventListener eventListener);
}
