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
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);

        coordinator = new Coordinator(pluginConfig, brokerContext);
        coordinator.run();

        //启动核心节点服务监听
        if (pluginConfig.isCore()) {
            httpServer = FeatCloud.cloudServer(cloudOptions -> cloudOptions.registerBean("mqttSession", coordinator.mqttSession).registerBean("brokerContext", brokerContext).host(pluginConfig.getHost()).port(pluginConfig.getPort()).debug(true)).listen();
        }
    }


    @Override
    protected void destroyPlugin() {
        //停止核心节点服务
        if (httpServer != null) {
            httpServer.shutdown();
            httpServer = null;
        }

        if (coordinator != null) {
            coordinator.destroy();
            coordinator = null;
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
        return "cluster-plugin";
    }

}
