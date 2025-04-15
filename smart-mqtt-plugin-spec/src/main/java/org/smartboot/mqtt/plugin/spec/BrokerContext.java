/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec;

import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.plugin.spec.bus.EventBus;
import org.smartboot.mqtt.plugin.spec.bus.MessageBus;
import org.smartboot.mqtt.plugin.spec.provider.Providers;
import org.smartboot.socket.timer.Timer;

import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public interface BrokerContext {


    Options Options();

    MqttSession getSession(String clientId);

    /**
     * 获取Topic，如果不存在将创建
     *
     * @param topic
     * @return
     */
    BrokerTopic getOrCreateTopic(String topic);

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

    Providers getProviders();

    /**
     * 解析配置文件
     */
    <T> T parseConfig(String path, Class<T> clazz);

    Map<Class<? extends MqttMessage>, MqttProcessor<?>> getMessageProcessors();

}
