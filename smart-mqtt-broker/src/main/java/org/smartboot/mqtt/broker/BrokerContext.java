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
import org.smartboot.mqtt.broker.provider.Providers;
import org.smartboot.mqtt.common.enums.MqttMetricEnum;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.to.MetricItemTO;

import java.io.IOException;
import java.util.Collection;
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

    void addSession(MqttSession session);

    MqttSession removeSession(String clientId);

    MqttSession getSession(String clientId);

    Collection<MqttSession> getSessions();

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

    ScheduledExecutorService getKeepAliveThreadPool();

    void destroy();

    Providers getProviders();

    /**
     * Broker运行时
     */
    BrokerRuntime getRuntime();

    /**
     * 解析配置文件
     */
    <T> T parseConfig(String path, Class<T> clazz);

    MqttBrokerMessageProcessor getMessageProcessor();

    /**
     * 运行指标
     */
    MetricItemTO metric(MqttMetricEnum metricEnum);

}
