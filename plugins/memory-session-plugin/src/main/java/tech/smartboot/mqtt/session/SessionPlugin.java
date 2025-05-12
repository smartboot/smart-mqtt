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
        this.brokerContext = brokerContext;
        preSessionStateProvider = brokerContext.getProviders().getSessionStateProvider();
        brokerContext.getProviders().setSessionStateProvider(new MemorySessionStateProvider());
    }

    @Override
    protected void destroyPlugin() {
        brokerContext.getProviders().setSessionStateProvider(preSessionStateProvider);
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
