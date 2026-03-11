package tech.smartboot.mqtt.bridge.redis;


import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.schema.Enum;
import tech.smartboot.mqtt.plugin.spec.schema.Item;
import tech.smartboot.mqtt.plugin.spec.schema.Schema;

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
        log("正在初始化 Redis 桥接插件...");
        BridgeConfig config = loadPluginConfig(BridgeConfig.class);

        int successCount = 0;
        for (BridgeConfig.RedisConfig redis : config.getRedis()) {
            if (MqttUtil.isBlank(redis.getAddress())) {
                log("Redis 地址为空，跳过该配置");
                continue;
            }
            if (bridges.containsKey(redis.getAddress())) {
                log("Redis 地址重复: " + redis.getAddress());
                continue;
            }
            log("正在连接 Redis: " + redis.getAddress());
            BridgeService service = new BridgeService(redis, brokerContext);
            service.start();
            bridges.put(redis.getAddress(), service);
            successCount++;
            log("Redis 连接成功: " + redis.getAddress());
        }
        log("Redis 桥接插件初始化完成，成功连接 " + successCount + " 个 Redis 实例");
    }

    @Override
    protected void destroyPlugin() {
        log("正在关闭 Redis 桥接插件...");
        bridges.values().forEach(BridgeService::destroy);
        log("Redis 桥接插件已关闭，已断开 " + bridges.size() + " 个 Redis 连接");
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

    @Override
    public Schema schema() {
        Schema schema = new Schema();
        Item array = Item.ItemArray("redis", "redis服务配置").col(12);
        array.addItems(Item.String("address", "redis服务地址").col(6), Item.Int("database", "redis数据库").col(6), Item.String("password", "redis密码").col(6), Item.String("encode", "redis编码").col(6).addEnums(Enum.of("bytes", "bytes"), Enum.of("string", "string"), Enum.of("base64", "base64")));
        schema.addItem(array);
        return schema;
    }
}
