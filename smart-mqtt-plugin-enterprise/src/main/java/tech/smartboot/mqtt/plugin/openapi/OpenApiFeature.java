/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.mqtt.plugin.AbstractFeature;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

import java.io.File;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * OpenApi服务升级
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
public class OpenApiFeature extends AbstractFeature {
    private static final String CONFIG_JSON_PATH = "$['broker']['openapi']";
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiFeature.class);

    private AsynchronousChannelGroup asynchronousChannelGroup;

    private final OpenApiConfig openApiConfig;

    private ExecutorService asyncExecutor;
    private File storage;

    public OpenApiFeature(BrokerContext context, File storage) {
        super(context);
        openApiConfig = context.parseConfig(CONFIG_JSON_PATH, OpenApiConfig.class);
        this.storage = storage;
    }

    private HttpServer httpServer;

    @Override
    public String name() {
        return "OpenApi";
    }

    @Override
    public void start() throws Exception {
        OpenApiConfig config = openApiConfig;
        if (config == null) {
            LOGGER.warn("openapi config is unset, init default config");
            config = new OpenApiConfig();
        }
        LOGGER.debug("openapi config:{}", JSONObject.toJSONString(config));
        asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            int i;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "openApi-" + (++i));
            }
        });

        asyncExecutor = Executors.newCachedThreadPool();
        OpenApiConfig finalConfig = config;
        httpServer = FeatCloud.cloudServer(serverOptions ->
                serverOptions.addExternalBean("brokerContext", this.context)
                        .addExternalBean("openApiConfig", finalConfig)
                        .addExternalBean("storage", storage)
                        .debug(true)
                        .bannerEnabled(false).threadNum(4).readBufferSize(1024 * 8).group(asynchronousChannelGroup));
        httpServer.listen(config.getHost(), config.getPort());
        LOGGER.debug("openapi server start success!");
        System.out.println("openapi server start success!");

    }

    @Override
    public void destroy() {
        httpServer.shutdown();
        asynchronousChannelGroup.shutdown();
//        IOUtil.shutdown(asynchronousChannelGroup);
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }
}
