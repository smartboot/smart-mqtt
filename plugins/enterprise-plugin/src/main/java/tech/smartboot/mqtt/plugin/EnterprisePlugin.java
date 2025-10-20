/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin;

import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public class EnterprisePlugin extends Plugin {
    public static final Timer SelfRescueTimer = new HashedWheelTimer(r -> {
        Thread t = new Thread(r, "self-rescue-timer");
        t.setDaemon(true);
        return t;
    }, 1000, 16);
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterprisePlugin.class);
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private HttpServer httpServer;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Exception {
        if ("false".equals(System.getenv("ENTERPRISE_ENABLE"))) {
            LOGGER.info("enterprise plugin disabled");
            return;
        }

        PluginConfig config = loadPluginConfig(PluginConfig.class);
        addUsagePort(config.getHttp().getPort(), (FeatUtils.isBlank(config.getHttp().getHost()) ? "0.0.0.0" : config.getHttp().getHost()) + ":" + config.getHttp().getPort());

        brokerContext.Options().addPlugin(new MonitorPlugin<>(60));

        asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            int i;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "openApi-" + (++i));
            }
        });

        httpServer = FeatCloud.cloudServer(serverOptions -> serverOptions.registerBean("brokerContext", brokerContext).registerBean("pluginConfig", config).registerBean("storage", storage()).group(asynchronousChannelGroup));
        httpServer.listen(config.getHttp().getHost(), config.getHttp().getPort());
        System.out.println("openapi server start success!");
    }

    @Override
    protected void destroyPlugin() {
        if (httpServer != null) {
            httpServer.shutdown();
        }

        asynchronousChannelGroup.shutdown();
    }


    @Override
    public int order() {
        return super.order() + 1;
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
        return "smart-mqtt 企业版";
    }

    @Override
    public String pluginName() {
        return "enterprise-plugin";
    }
}
