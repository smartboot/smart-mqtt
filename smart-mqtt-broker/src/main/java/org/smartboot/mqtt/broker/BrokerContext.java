/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.broker.provider.Providers;
import org.smartboot.mqtt.broker.topic.TopicPublishTree;
import org.smartboot.mqtt.broker.topic.TopicSubscribeTree;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.timer.Timer;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

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

    void addSession(MqttSession session);

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
     * 获取消息总线
     *
     * @return
     */
    MessageBus getMessageBus();

    /**
     * 获取事件总线
     *
     * @return
     */
    EventBus getEventBus();

    Timer getTimer();

    void destroy();

    Providers getProviders();

    /**
     * 解析配置文件
     */
    <T> T parseConfig(String path, Class<T> clazz);

    <T extends MqttMessage> Map<Class<? extends MqttMessage>, MqttProcessor<?>> getMessageProcessors();

    TopicPublishTree getPublishTopicTree();

    TopicSubscribeTree getTopicSubscribeTree();

    <T> void bundle(String key, T resource);

    <T> T getBundle(String key);
}
