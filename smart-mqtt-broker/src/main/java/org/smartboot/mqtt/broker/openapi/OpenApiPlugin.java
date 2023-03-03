package org.smartboot.mqtt.broker.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.restful.RestfulBootstrap;
import org.smartboot.http.restful.StaticResourceHandler;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.openapi.controller.ConnectionsController;
import org.smartboot.mqtt.broker.openapi.controller.DashBoardController;
import org.smartboot.mqtt.broker.openapi.controller.SubscriptionController;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.PluginException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OpenApiPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['broker']['openapi']";

    @Override
    protected void initPlugin(BrokerContext brokerContext) {
        Config config = brokerContext.parseConfig(CONFIG_JSON_PATH, Config.class);
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            return;
        }
        try {
            RestfulBootstrap restfulBootstrap = RestfulBootstrap.getInstance(new StaticResourceHandler());
            restfulBootstrap.inspect((httpRequest, response) -> {
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Headers", "*");
            });
            restfulBootstrap.controller(new DashBoardController(brokerContext));
            restfulBootstrap.controller(new ConnectionsController());
            restfulBootstrap.controller(new SubscriptionController());

            HttpBootstrap bootstrap = restfulBootstrap.bootstrap();
            bootstrap.setPort(config.getPort());
            bootstrap.configuration().bannerEnabled(false).host(config.getHost()).readBufferSize(1024 * 8);
            bootstrap.start();
            brokerContext.getProviders().setOpenApiBootStrap(restfulBootstrap);
            LOGGER.info("openapi server start success!");
        } catch (Exception e) {
            LOGGER.error("start openapi exception", e);
            throw new PluginException("start openapi exception");
        }
    }
}
