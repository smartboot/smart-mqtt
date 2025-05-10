/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import tech.smartboot.mqtt.broker.topic.BaseMessageDeliver;
import tech.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import tech.smartboot.mqtt.common.TopicToken;
import tech.smartboot.mqtt.common.enums.MqttQoS;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT客户端的主题订阅关系类，支持精确匹配和通配符订阅。
 * <p>
 * 该类维护了客户端订阅的主题过滤器（Topic Filter）信息，包括：
 * <ul>
 *   <li>主题过滤器的Token解析结果，支持通配符（+和#）匹配</li>
 *   <li>订阅的QoS级别，决定消息传递的服务质量</li>
 *   <li>匹配的具体主题集合，尤其在通配符订阅时可能对应多个实际主题</li>
 * </ul>
 * </p>
 * <p>
 * 通配符订阅机制：
 * <ul>
 *   <li>'+' 通配符：匹配单个层级的任意字符</li>
 *   <li>'#' 通配符：匹配多个层级的任意字符</li>
 *   <li>例如：'sensor/+/temp'可匹配'sensor/1/temp'、'sensor/2/temp'等</li>
 * </ul>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/13
 */
public class TopicSubscription {
    /**
     * 主题过滤器的Token解析结果。
     * <p>
     * 存储了订阅主题的层级结构和通配符信息，用于后续的主题匹配判断。
     * 对于通配符订阅，此Token包含了通配符的位置和类型信息。
     * </p>
     */
    private final TopicToken topicFilterToken;

    /**
     * 订阅的QoS级别。
     * <p>
     * 决定了消息传递的服务质量保证：
     * <ul>
     *   <li>QoS 0：最多一次传递</li>
     *   <li>QoS 1：至少一次传递</li>
     *   <li>QoS 2：确保一次传递</li>
     * </ul>
     * </p>
     */
    private MqttQoS mqttQoS;

    /**
     * 客户端订阅所匹配的具体主题集合。
     * <p>
     * 对于通配符订阅，一个主题过滤器可能匹配多个实际的主题。
     * Key为实际的主题对象（BrokerTopic），Value为该主题的消费记录。
     * 此Map在主题匹配时动态维护，随着主题的发布和取消订阅而更新。
     * </p>
     */
    private final Map<BrokerTopicImpl, BaseMessageDeliver> topicSubscribers = new HashMap<>();

    /**
     * 创建主题订阅关系实例。
     *
     * @param topicFilterToken 主题过滤器的Token解析结果
     * @param mqttQoS          订阅的QoS级别
     */
    public TopicSubscription(TopicToken topicFilterToken, MqttQoS mqttQoS) {
        this.topicFilterToken = topicFilterToken;
        this.mqttQoS = mqttQoS;
    }

    /**
     * 获取主题过滤器的Token解析结果。
     *
     * @return 主题过滤器的Token对象，包含主题层级和通配符信息
     */
    public TopicToken getTopicFilterToken() {
        return topicFilterToken;
    }

    /**
     * 获取订阅的QoS级别。
     *
     * @return 当前订阅的QoS级别
     */
    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    /**
     * 设置订阅的QoS级别。
     * <p>
     * 当客户端重新订阅相同主题但QoS不同时，用于更新QoS级别。
     * </p>
     *
     * @param mqttQoS 新的QoS级别
     */
    public void setMqttQoS(MqttQoS mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    /**
     * 获取订阅匹配的具体主题集合。
     * <p>
     * 返回当前主题过滤器匹配的所有实际主题及其消费记录。
     * 对于通配符订阅，此集合可能包含多个匹配的主题。
     * </p>
     *
     * @return 主题与消费记录的映射关系
     */
    public Map<BrokerTopicImpl, BaseMessageDeliver> getTopicSubscribers() {
        return topicSubscribers;
    }
}

