/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.plugin.ws;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgrade;
import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.broker.MqttSessionImpl;
import tech.smartboot.mqtt.common.MqttProtocol;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttProcessor;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 4/15/25
 */
public class WebSocketPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketPlugin.class);
    private HttpServer httpBootstrap;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        Config config = brokerContext.parseConfig("$.broker.ws", Config.class);
        if (config == null) {
            LOGGER.error("mqtt over websocket is disable.");
            return;
        }
        LOGGER.debug("websocket config:{}", JSONObject.toJSONString(config));
        MqttProtocol protocol = new MqttProtocol(brokerContext.Options().getMaxPacketSize());
        httpBootstrap = Feat.httpServer(serverOptions -> serverOptions.debug(true).bannerEnabled(false).readBufferSize(1024 * 1024)).httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                request.getResponse().setHeader("Sec-WebSocket-Protocol", "mqtt");
                request.upgrade(new WebSocketUpgrade() {
                    ProxySession proxySession;
                    ByteBuffer readBuffer;
                    MqttSessionImpl mqttSession;

                    @Override
                    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
                        if (proxySession == null) {
                            proxySession = new ProxySession(this.request.getAioSession());
                            mqttSession = new MqttSessionImpl((BrokerContextImpl) brokerContext, proxySession, new ByteArrayMqttOutputStream(response));
                            proxySession.setAttachment(mqttSession);
                            readBuffer = ByteBuffer.allocate(brokerContext.Options().getBufferSize());
                        }
                        readBuffer.put(data);
                        readBuffer.flip();
                        while (readBuffer.hasRemaining()) {
                            MqttMessage message = protocol.decode(readBuffer, proxySession);
                            if (message == null) {
                                break;
                            }
                            //处理消息
                            MqttProcessor processor = brokerContext.getMessageProcessors().get(message.getClass());
                            if (processor != null) {
                                brokerContext.getEventBus().publish(EventType.RECEIVE_MESSAGE, EventObject.newEventObject(mqttSession, message));
                                mqttSession.setLatestReceiveMessageTime(System.currentTimeMillis());
                                processor.process(brokerContext, mqttSession, message);
                            } else {
                                System.err.println("unSupport message: " + data);
                            }
                        }
                        readBuffer.compact();
                    }
                });
            }
        });
        httpBootstrap.listen(config.getPort());
    }

    @Override
    protected void destroyPlugin() {
        if (httpBootstrap != null) {
            httpBootstrap.shutdown();
        }
    }

    @Override
    public String getVersion() {
        return Options.VERSION;
    }

    @Override
    public String getVendor() {
        return Options.VENDOR;
    }

    @Override
    public String getDescription() {
        return "mqtt over websocket";
    }
}
