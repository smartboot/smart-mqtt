package org.smartboot.mqtt.broker.openapi.controller;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.enums.ConnectionStatusEnum;
import org.smartboot.mqtt.broker.openapi.to.ConnectionTO;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
@Controller
public class ConnectionsController {
    private final BrokerContext brokerContext;
    private ConcurrentHashMap<String, ConnectionTO> connections = new ConcurrentHashMap<>();

    public ConnectionsController(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
        brokerContext.getEventBus().subscribe(ServerEventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();
            MqttConnectMessage message = object.getObject();
            ConnectionTO connection = new ConnectionTO();
            connection.setClientId(session.getClientId());
            connection.setUsername(message.getPayload().userName());
            try {
                connection.setIpAddress(session.getRemoteAddress().getHostName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            connection.setStatus(ConnectionStatusEnum.CONNECTED.getStatus());
            connection.setCleanStart(message.getVariableHeader().isCleanSession());
            connection.setKeepalive(message.getVariableHeader().keepAliveTimeSeconds());
            connection.setConnectTime(new Date());
            connections.put(session.getClientId(), connection);
        });
        brokerContext.getEventBus().subscribe(ServerEventType.DISCONNECT, new EventBusSubscriber<AbstractSession>() {
            @Override
            public void subscribe(EventType<AbstractSession> eventType, AbstractSession object) {
                connections.remove(object.getClientId());
            }
        });
    }

    @RequestMapping(OpenApi.CONNECTIONS)
    public RestResult<List<ConnectionTO>> connections(HttpResponse response) {
        return RestResult.ok(connections.values().stream().sorted(Comparator.comparing(ConnectionTO::getConnectTime)).collect(Collectors.toList()));
    }
}
