package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public class ClusterPlugin extends Plugin {
    public static final EventType<Message> CLIENT_DIRECT_TO_CORE_BROKER = new EventType<>("client_direct_to_core_broker");

    private HttpServer httpServer;

    private Coordinator coordinator;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        log("正在初始化集群插件...");
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);

        //启动协调者
        coordinator = new Coordinator(pluginConfig, brokerContext);
        Thread thread = new Thread(coordinator, "cluster-plugin-coordinator");
        thread.setDaemon(true);
        thread.start();

        //启动核心节点服务监听
        if (pluginConfig.isCore()) {
            addUsagePort(pluginConfig.getPort(), "cluster coreNode port");
            httpServer = FeatCloud.cloudServer(cloudOptions -> cloudOptions.registerBean("plugin", this).registerBean("mqttSession", coordinator.mqttSession).registerBean("brokerContext", brokerContext).host(pluginConfig.getHost()).port(pluginConfig.getPort()).debug(true)).listen();
            log("集群核心节点服务已启动，监听端口: " + pluginConfig.getPort());
        } else {
            log("集群边缘节点模式启动");
        }
        log("集群插件初始化完成");
    }


    @Override
    protected void destroyPlugin() {
        log("正在关闭集群插件...");
        //停止核心节点服务
        if (httpServer != null) {
            httpServer.shutdown();
            httpServer = null;
            log("集群核心节点服务已停止");
        }

        if (coordinator != null) {
            coordinator.destroy();
            coordinator = null;
        }
        log("集群插件已关闭");
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
        return "cluster-plugin";
    }

}
