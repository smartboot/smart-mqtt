/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.protocol;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.EventType;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.MqttMessageBuilders;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GatewayService {

    private final ConcurrentHashMap<Object, AioSession> sessions = new ConcurrentHashMap<>();

    private final BrokerContext brokerContext;

    private final MqttClient client;

    GatewayService(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
        client = new MqttClient("127.0.0.1", brokerContext.getBrokerConfigure().getPort(), MqttUtil.createClientId());
        client.getClientConfigure().setConnectionTimeout(3000);
        client.connect(brokerContext.getBrokerConfigure().getChannelGroup());
    }


    /**
     * 订阅来自Broker推送的下行消息
     *
     * @param topicFilter
     * @param subscribed
     */
    public void subscribeDownStream(String topicFilter, DownStream subscribed) {
        client.subscribe(topicFilter, MqttQoS.AT_MOST_ONCE, (mqttClient, message) -> subscribed.onStream(message.getVariableHeader().getTopicName(), message.getPayload().getPayload()));
    }

    public final void sendToClient(String key, Consumer<AioSession> consumer) {
        consumer.accept(getSession(key));
    }

    public final void sendToBroker(String topic, byte[] payload) {
        MqttPublishMessage message = MqttMessageBuilders.publish()
                .qos(MqttQoS.AT_MOST_ONCE)
                .topicName(topic).payload(payload).build();
        brokerContext.getEventBus().publish(EventType.RECEIVE_PUBLISH_MESSAGE, EventObject.newEventObject(client, message));
    }

    public void addSession(String key, AioSession session) {
        sessions.put(key, session);
    }

    public AioSession getSession(String key) {
        return sessions.get(key);
    }

    public void removeSession(String key) {
        sessions.remove(key);
    }
}
