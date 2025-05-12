package tech.smartboot.mqtt.bridge.redis;

import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.mqtt.common.enums.PayloadEncodeEnum;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

class BridgeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeService.class);
    private final BridgeConfig.RedisConfig config;
    private RedissonClient redissonClient;
    private final BrokerContext context;

    private boolean enable = true;

    public BridgeService(BridgeConfig.RedisConfig config, BrokerContext context) {
        this.config = config;
        this.context = context;
    }

    public void start() {
        // 创建Kafka生产者对象，指定Kafka集群地址和端口号
        Config c = new Config();
        c.useSingleServer().setAddress(config.getAddress()).setDatabase(config.getDatabase()).setPassword(config.getPassword());
        redissonClient = Redisson.create(c);
        LOGGER.info("start redis bridge");
        PayloadEncodeEnum encodeEnum = PayloadEncodeEnum.getEnumByCode(config.getEncode());
        context.getMessageBus().consumer(new MessageBusConsumer() {
            @Override
            public void consume(MqttSession session, Message publishMessage) {
                RScoredSortedSet<String> bucket = redissonClient.getScoredSortedSet(publishMessage.getTopic().getTopic(), StringCodec.INSTANCE);
                boolean suc = bucket.add(System.currentTimeMillis(), publishMessage.getJsonObject(encodeEnum));
                if (!suc) {
                    System.err.println("redis bridge error");
                }
            }

            @Override
            public boolean enable() {
                return enable;
            }
        });
    }


    public void destroy() {
        enable = false;
        redissonClient.shutdown();
    }

}
