package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.listener.BrokerListeners;
import org.smartboot.mqtt.broker.plugin.provider.Providers;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

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

    MqttSession removeSession(String clientId);

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
    void publish(MqttSession session, MqttPublishMessage message);

    /**
     * 推送retain消息至客户端
     *
     * @param session
     */
    void publishRetain(TopicSubscriber session);

    ScheduledExecutorService getKeepAliveThreadPool();

    void destroy();

    Providers getProviders();

    BrokerListeners getListeners();

    void addEvent(EventListener eventListener);
}
