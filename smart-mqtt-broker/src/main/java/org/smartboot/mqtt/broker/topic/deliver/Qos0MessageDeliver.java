/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.topic.deliver;

import org.smartboot.mqtt.broker.TopicSubscription;
import org.smartboot.mqtt.broker.topic.BrokerTopicImpl;
import org.smartboot.mqtt.common.enums.MqttVersion;
import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;
import org.smartboot.mqtt.plugin.spec.MqttSession;
import org.smartboot.mqtt.plugin.spec.PublishBuilder;
import org.smartboot.mqtt.plugin.spec.bus.Message;

/**
 * MQTT主题消费者记录类，负责管理单个订阅者的消息消费状态和推送逻辑。
 * <p>
 * 该类继承自AbstractConsumerRecord，主要功能包括：
 * <ul>
 *   <li>维护订阅者的会话状态和QoS级别</li>
 *   <li>实现消息的推送机制，确保消息按序推送到客户端</li>
 *   <li>支持MQTT 5.0协议的特性，如消息属性</li>
 * </ul>
 * </p>
 * <p>
 * 消息推送机制：
 * <ul>
 *   <li>通过nextConsumerOffset追踪消息消费位置</li>
 *   <li>支持批量推送，但限制单次推送数量（最多100条）</li>
 *   <li>自动处理会话断开和禁用状态</li>
 * </ul>
 * </p>
 * <p>
 * QoS保证：
 * <ul>
 *   <li>支持不同QoS级别的消息推送</li>
 *   <li>QoS 0：最多一次，直接推送无需确认</li>
 *   <li>QoS 1和2：通过会话的消息队列和确认机制保证可靠性</li>
 * </ul>
 * </p>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/25
 */
public class Qos0MessageDeliver extends AbstractMessageDeliver {
    /**
     * MQTT客户端会话对象，维护与订阅客户端的连接状态和通信通道。
     * 用于消息推送和会话状态检查。
     */
    protected final MqttSession mqttSession;

    public Qos0MessageDeliver(BrokerTopicImpl topic, MqttSession session, TopicSubscription topicSubscription, long nextConsumerOffset) {
        super(topic, topicSubscription, nextConsumerOffset);
        this.mqttSession = session;
    }

    /**
     * 将消息推送到订阅客户端。
     * <p>
     * 该方法实现了消息的批量推送机制：
     * <ul>
     *   <li>检查会话状态和启用状态</li>
     *   <li>批量推送消息，但限制最大推送次数为100</li>
     *   <li>超过限制时重新加入订阅队列等待下次推送</li>
     *   <li>推送完成后刷新会话缓冲区</li>
     * </ul>
     * </p>
     */
    public void pushToClient() {
        if (mqttSession.isDisconnect() || !enable) {
            return;
        }
        int i = 0;
        while (push0()) {
            if (i++ > 100) {
                topic.addSubscriber(this);
                topic.addVersion();
                break;
            }
        }
        mqttSession.flush();
    }

    /**
     * 执行单条消息的推送操作。
     * <p>
     * 该方法负责：
     * <ul>
     *   <li>获取下一条待推送的消息</li>
     *   <li>构建MQTT PUBLISH消息，设置QoS和主题</li>
     *   <li>处理MQTT 5.0的消息属性</li>
     *   <li>更新消费位置并提交确认</li>
     * </ul>
     * </p>
     *
     * @return 如果成功推送消息返回true，如果没有新消息返回false
     */
    private boolean push0() {
        Message message = topic.getMessageQueue().get(nextConsumerOffset);
        if (message == null) {
            topic.addSubscriber(this);
            return false;
        }

        PublishBuilder publishBuilder = PublishBuilder.builder().payload(message.getPayload()).qos(getMqttQoS()).topic(message.getTopic());
        if (mqttSession.getMqttVersion() == MqttVersion.MQTT_5) {
            publishBuilder.publishProperties(new PublishProperties());
        }

        nextConsumerOffset = message.getOffset() + 1;
        topic.getMessageQueue().commit(message.getOffset());
        mqttSession.write(publishBuilder.build(), false);
        return true;
    }

    public final MqttSession getMqttSession() {
        return mqttSession;
    }
}
