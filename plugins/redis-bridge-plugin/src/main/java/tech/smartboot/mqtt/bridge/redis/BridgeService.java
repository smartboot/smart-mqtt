package tech.smartboot.mqtt.bridge.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.mqtt.common.enums.PayloadEncodeEnum;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;
import tech.smartboot.redisun.Redisun;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

class BridgeService {
    private final BridgeConfig.RedisConfig config;
    private Redisun redisun;
    private final BrokerContext context;

    private boolean enable = true;

    public BridgeService(BridgeConfig.RedisConfig config, BrokerContext context) {
        this.config = config;
        this.context = context;
    }

    public void start() {
        // 创建Kafka生产者对象，指定Kafka集群地址和端口号
        redisun = Redisun.create(opt -> opt.debug(true).setAddress(config.getAddress()).setDatabase(config.getDatabase()).setPassword(config.getPassword()));
        PayloadEncodeEnum encodeEnum = PayloadEncodeEnum.getEnumByCode(config.getEncode());
        context.getMessageBus().consumer(new MessageBusConsumer() {
            @Override
            public void consume(MqttSession session, Message publishMessage) {
                long timestamp = System.currentTimeMillis();
                JSONObject json = toJsonString(publishMessage, encodeEnum);
                json.put("timestamp", timestamp);
                CompletableFuture<Integer> future = redisun.asyncZadd(publishMessage.getTopic().getTopic(), timestamp, json.toString());
                future.exceptionally(throwable -> {
                    System.err.println("redis bridge error");
                    return null;
                });
            }

            @Override
            public boolean enable() {
                return enable;
            }
        });
    }

    public JSONObject toJsonString(Message message, PayloadEncodeEnum payloadEncodeEnum) {
        if (payloadEncodeEnum == null) {
            payloadEncodeEnum = PayloadEncodeEnum.BYTES;
        }
        switch (payloadEncodeEnum) {
            case STRING: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("payload", new String(message.getPayload()));
                json.put("encoding", payloadEncodeEnum.getCode());
                return json;
            }
            case BASE64: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("payload", new String(Base64.getEncoder().encode(message.getPayload())));
                json.put("encoding", payloadEncodeEnum.getCode());
                return json;
            }
            default: {
                JSONObject json = (JSONObject) JSON.toJSON(this);
                json.put("encoding", payloadEncodeEnum.getCode());
                return json;
            }
        }
    }

    public void destroy() {
        enable = false;
        redisun.close();
    }

}
