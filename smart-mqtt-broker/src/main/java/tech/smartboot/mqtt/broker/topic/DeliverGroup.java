/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.mqtt.broker.topic.deliver.AbstractMessageDeliver;
import tech.smartboot.mqtt.plugin.spec.MessageDeliver;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT主题订阅组，负责管理特定主题的订阅者集合。
 * <p>
 * 该类是MQTT Broker中订阅管理的基础组件，主要用于：
 * <ul>
 *   <li>管理订阅者会话与消费者记录的映射关系</li>
 *   <li>支持订阅者的动态添加和移除</li>
 *   <li>提供订阅者数量统计功能</li>
 * </ul>
 * </p>
 * <p>
 * 订阅组的核心是一个线程安全的Map结构（subscribers），用于存储：
 * <ul>
 *   <li>Key: MQTT客户端会话（MqttSession）</li>
 *   <li>Value: 主题消费者记录（TopicConsumerRecord）</li>
 * </ul>
 * 使用ConcurrentHashMap实现，确保在高并发场景下的线程安全。
 * </p>
 * <p>
 * 该类可以被继承以支持不同类型的订阅模式，如：
 * <ul>
 *   <li>普通订阅 - 每个订阅者都收到所有消息</li>
 *   <li>共享订阅 - 消息以负载均衡方式分发给订阅者</li>
 * </ul>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 */
public class DeliverGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliverGroup.class);

    /**
     * 订阅者映射表，存储会话与消费者记录的对应关系。
     * <p>
     * 使用ConcurrentHashMap实现线程安全的并发访问：
     * <ul>
     *   <li>Key: MQTT客户端会话对象，代表一个连接的客户端</li>
     *   <li>Value: 主题消费者记录，包含订阅相关的配置和状态</li>
     * </ul>
     * </p>
     */
    protected final Map<MqttSession, AbstractMessageDeliver> subscribers = new ConcurrentHashMap<>();

    /**
     * 获取指定会话的订阅者记录。
     *
     * @param session MQTT客户端会话
     * @return 返回该会话对应的消费者记录，如果不存在则返回null
     */
    public MessageDeliver getMessageDeliver(MqttSession session) {
        return subscribers.get(session);
    }

    /**
     * 移除指定会话的订阅关系。
     *
     * @param session 要移除订阅的MQTT客户端会话
     * @return 返回被移除的消费者记录，如果不存在则返回null
     */
    public AbstractMessageDeliver removeMessageDeliver(MqttSession session) {
        AbstractMessageDeliver deliver = subscribers.remove(session);
        if (deliver != null) {
            deliver.disable();
        }
        return deliver;
    }

    /**
     * 添加新的订阅关系。
     *
     * @param subscriber 要添加的主题消费者记录，包含会话信息和订阅配置
     */
    public void addMessageDeliver(AbstractMessageDeliver subscriber) {
        subscribers.put(subscriber.getMqttSession(), subscriber);
    }

    /**
     * 获取当前订阅组中的订阅者数量。
     *
     * @return 返回当前活跃的订阅者数量
     */
    public int count() {
        return subscribers.size();
    }

    public boolean isShared() {
        return false;
    }
}
