/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus.messagebus.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.messagebus.Message;
import org.smartboot.mqtt.broker.topic.BrokerTopic;
import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * Retain消息持久化
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class RetainPersistenceConsumer implements Consumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainPersistenceConsumer.class);
    private final BrokerContext brokerContext;

    public RetainPersistenceConsumer(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @Override
    public void consume(MqttSession session, Message message) {
        if (!message.isRetained()) {
            return;
        }
        BrokerTopic topic = brokerContext.getOrCreateTopic(message.getTopic());
        //保留标志为 1 且有效载荷为零字节的 PUBLISH 报文会被服务端当作正常消息处理，它会被发送给订阅主题匹配的客户端。
        // 此外，同一个主题下任何现存的保留消息必须被移除，因此这个主题之后的任何订阅者都不会收到一个保留消息。
        if (message.getPayload().length == 0) {
            LOGGER.info("clear topic:{} retained messages, because of current retained message's payload length is 0", message.getTopic());
            topic.setRetainMessage(null);
            return;
        }
        /*
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (message.getQos() == MqttQoS.AT_MOST_ONCE) {
            LOGGER.info("receive Qos0 retain message,clear topic:{} retained messages", message.getTopic());
            topic.setRetainMessage(null);
        }
        topic.setRetainMessage(message);
    }
}
