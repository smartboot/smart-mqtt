/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.restful.RestfulBootstrap;
import org.smartboot.http.restful.StaticResourceHandler;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.openapi.controller.BrokerController;
import org.smartboot.mqtt.broker.openapi.controller.ConnectionsController;
import org.smartboot.mqtt.broker.openapi.controller.DashBoardController;
import org.smartboot.mqtt.broker.openapi.controller.SubscriptionController;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.PluginException;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OpenApiPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['broker']['openapi']";
    private RestfulBootstrap restfulBootstrap;

    private AsynchronousChannelGroup asynchronousChannelGroup;

    @Override
    protected void initPlugin(BrokerContext brokerContext) {
        Config config = brokerContext.parseConfig(CONFIG_JSON_PATH, Config.class);
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            return;
        }
        try {
            asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                int i;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "openApi-" + (++i));
                }
            });
            restfulBootstrap = RestfulBootstrap.getInstance(new StaticResourceHandler());
            restfulBootstrap.inspect((httpRequest, response) -> {
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Headers", "*");
            });
            restfulBootstrap.controller(new DashBoardController(brokerContext));
            restfulBootstrap.controller(new ConnectionsController());
            restfulBootstrap.controller(new SubscriptionController());
            restfulBootstrap.controller(new BrokerController());

            HttpBootstrap bootstrap = restfulBootstrap.bootstrap();
            bootstrap.setPort(config.getPort());
            bootstrap.configuration().bannerEnabled(false).host(config.getHost()).readBufferSize(1024 * 8).group(asynchronousChannelGroup);

            bootstrap.start();
            brokerContext.getProviders().setOpenApiBootStrap(restfulBootstrap);
            LOGGER.info("openapi server start success!");
        } catch (Exception e) {
            LOGGER.error("start openapi exception", e);
            throw new PluginException("start openapi exception");
        }
    }

    @Override
    protected void destroyPlugin() {
        restfulBootstrap.bootstrap().shutdown();
        asynchronousChannelGroup.shutdown();
    }
}
