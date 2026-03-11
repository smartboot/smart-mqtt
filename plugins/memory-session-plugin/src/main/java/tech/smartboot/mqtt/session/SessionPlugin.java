package tech.smartboot.mqtt.session;

import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.provider.SessionStateProvider;

/**
 * @author 三刀
 * @version v1.0 4/29/25
 */
public class SessionPlugin extends Plugin {
    private SessionStateProvider preSessionStateProvider;
    private BrokerContext brokerContext;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        log("正在初始化内存会话插件...");
        this.brokerContext = brokerContext;
        preSessionStateProvider = brokerContext.getProviders().getSessionStateProvider();
        brokerContext.getProviders().setSessionStateProvider(new MemorySessionStateProvider());
        log("内存会话插件初始化完成，已将会话状态存储切换为内存模式");
    }

    @Override
    protected void destroyPlugin() {
        log("正在关闭内存会话插件...");
        brokerContext.getProviders().setSessionStateProvider(preSessionStateProvider);
        log("内存会话插件已关闭，会话状态存储已恢复为原模式");
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
        return "memory-session-plugin";
    }
}
