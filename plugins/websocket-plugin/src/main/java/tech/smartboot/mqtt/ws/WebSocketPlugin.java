/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.ws;

import org.smartboot.socket.StateMachineEnum;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgrade;
import tech.smartboot.mqtt.common.MqttProtocol;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;

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
        PluginConfig config = loadPluginConfig(PluginConfig.class);
        addUsagePort(config.getPort(), "websocket port");
        MqttProtocol protocol = new MqttProtocol(brokerContext.Options().getMaxPacketSize());
        httpBootstrap = Feat.httpServer(serverOptions -> serverOptions.debug(true).bannerEnabled(false).readBufferSize(1024 * 1024)).httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                request.getResponse().setHeader("Sec-WebSocket-Protocol", "mqtt");
                request.upgrade(new WebSocketUpgrade() {
                    ProxySession proxySession;
                    ByteBuffer readBuffer;

                    @Override
                    public void onHandShake(WebSocketRequest request, WebSocketResponse response) {
                        super.onHandShake(request, response);
                        proxySession = new ProxySession(this.request.getAioSession(), response);
                        readBuffer = ByteBuffer.allocate(brokerContext.Options().getBufferSize());
                        brokerContext.Options().getProcessor().stateEvent(proxySession, StateMachineEnum.NEW_SESSION, null);
                    }

                    @Override
                    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
                        readBuffer.put(data);
                        readBuffer.flip();
                        while (readBuffer.hasRemaining()) {
                            MqttMessage message = protocol.decode(readBuffer, proxySession);
                            if (message == null) {
                                break;
                            }
                            //处理消息
                            brokerContext.Options().getProcessor().process(proxySession, message);
                            proxySession.writeBuffer().flush();
                        }
                        readBuffer.compact();
                    }

                    @Override
                    public void destroy() {
                        super.destroy();
                        brokerContext.Options().getProcessor().stateEvent(proxySession, StateMachineEnum.SESSION_CLOSED, null);
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
    public String pluginName() {
        return "websocket-plugin";
    }
}
