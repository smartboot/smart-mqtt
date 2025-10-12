package tech.smartboot.mqtt.bridge.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import tech.smartboot.mqtt.common.enums.PayloadEncodeEnum;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.util.Base64;

class BridgeService {
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
        PayloadEncodeEnum encodeEnum = PayloadEncodeEnum.getEnumByCode(config.getEncode());
        context.getMessageBus().consumer(new MessageBusConsumer() {
            @Override
            public void consume(MqttSession session, Message publishMessage) {
                RScoredSortedSet<String> bucket = redissonClient.getScoredSortedSet(publishMessage.getTopic().getTopic(), StringCodec.INSTANCE);
                boolean suc = bucket.add(System.currentTimeMillis(), toJsonString(publishMessage, encodeEnum));
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

    public String toJsonString(Message message, PayloadEncodeEnum payloadEncodeEnum) {
        if (payloadEncodeEnum == null) {
            payloadEncodeEnum = PayloadEncodeEnum.BYTES;
        }
        switch (payloadEncodeEnum) {
            case STRING: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("payload", new String(message.getPayload()));
                json.put("encoding", payloadEncodeEnum.getCode());
                return json.toString();
            }
            case BASE64: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("payload", new String(Base64.getEncoder().encode(message.getPayload())));
                json.put("encoding", payloadEncodeEnum.getCode());
                return json.toString();
            }
            default: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("encoding", payloadEncodeEnum.getCode());
                return json.toString();
            }
        }
    }

    public void destroy() {
        enable = false;
        redissonClient.shutdown();
    }

}
