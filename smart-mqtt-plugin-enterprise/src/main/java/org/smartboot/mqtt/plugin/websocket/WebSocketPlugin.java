package org.smartboot.mqtt.plugin.websocket;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.EventObject;
import org.smartboot.mqtt.broker.eventbus.EventType;
import org.smartboot.mqtt.broker.processor.MqttProcessor;
import org.smartboot.mqtt.common.MqttProtocol;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgrade;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/1
 */
@Bean
public class WebSocketPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketPlugin.class);
    private HttpServer httpBootstrap;

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @PostConstruct
    public void init() {
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
                    MqttSession mqttSession;

                    @Override
                    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
                        if (proxySession == null) {
                            proxySession = new ProxySession(this.request.getAioSession());
                            mqttSession = new MqttSession(brokerContext, proxySession, new ByteArrayMqttOutputStream(response));
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

    @PreDestroy
    public void destroyPlugin() {
        if (httpBootstrap != null) {
            httpBootstrap.shutdown();
        }
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
