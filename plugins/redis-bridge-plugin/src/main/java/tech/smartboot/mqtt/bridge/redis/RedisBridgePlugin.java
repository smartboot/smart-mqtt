package tech.smartboot.mqtt.bridge.redis;


import org.apache.commons.lang.StringUtils;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/11/25
 */
public class RedisBridgePlugin extends Plugin {
    private final Map<String, BridgeService> bridges = new ConcurrentHashMap<>();

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        BridgeConfig config = loadPluginConfig(BridgeConfig.class);

        for (BridgeConfig.RedisConfig redis : config.getRedis()) {
            if (StringUtils.isBlank(redis.getAddress())) {
                System.err.println("redis address is blank");
                continue;
            }
            if (bridges.containsKey(redis.getAddress())) {
                System.err.println("duplicate redis" + redis.getAddress());
                continue;
            }
            BridgeService service = new BridgeService(redis, brokerContext);
            service.start();
            bridges.put(redis.getAddress(), service);
        }
    }

    @Override
    protected void destroyPlugin() {
        bridges.values().forEach(BridgeService::destroy);
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
        return "redis-bridge-plugin";
    }
}
