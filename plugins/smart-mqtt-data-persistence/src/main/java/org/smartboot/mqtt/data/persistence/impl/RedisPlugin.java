package org.smartboot.mqtt.data.persistence.impl;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.bus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.plugin.PluginException;
import org.smartboot.mqtt.data.persistence.DataPersistPlugin;
import org.smartboot.mqtt.data.persistence.config.imp.RedisPluginConfig;
import org.smartboot.mqtt.data.persistence.nodeinfo.MessageNodeInfo;
import org.smartboot.mqtt.data.persistence.utils.StrUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class RedisPlugin extends DataPersistPlugin<RedisPluginConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['plugins']['redis-bridge'][0]";
    private static final String MESSAGE_PREFIX = "smart-mqtt-message:";
    private static StrUtils<MessageNodeInfo> StrUtil = new StrUtils<>();

    private static AtomicInteger atomicInteger = new AtomicInteger(0);
    private static StatefulRedisConnection<String, String> CONNECTION;
    private static RedisClient CLIENT;
    private static RedisAsyncCommands<String, String> ASYNC_COMMAND;

    @Override
    protected RedisPluginConfig connect(BrokerContext brokerContext) {
        RedisPluginConfig config = brokerContext.parseConfig(CONFIG_JSON_PATH, RedisPluginConfig.class);
        // 启动加载redis的配置文件
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            throw new PluginException("start DataPersistRedisPlugin exception");
        }
        this.setConfig(config);

        LOGGER.info("redisPoll create success");
        RedisURI redisUri = RedisURI.builder()
                .withHost(config.getHost().split(":")[0])
                .withPort(Integer.parseInt(config.getHost().split(":")[1]))
                .withPassword(config.getPassword())
                .withTimeout(Duration.of(config.getTimeout(), ChronoUnit.SECONDS)).build();
        // 创建客户端，建立链接
        CLIENT = RedisClient.create(redisUri);
        // 建立链接
        CONNECTION = CLIENT.connect();
        // 构建命令实例
        ASYNC_COMMAND = CONNECTION.async();
        return config;
    }

    @Override
    protected void listenAndPushMessage(BrokerContext brokerContext, RedisPluginConfig config) {
        // 消息总线监听
        MessageBus messageBus = brokerContext.getMessageBus();
        // 客户端发送消息来了，到消息总线调用consumer方法
        long start = System.currentTimeMillis();
        messageBus.consumer((session, busMessage) -> {
            // 获得message信息Vo对象
            MessageNodeInfo messageNodeInfo = new MessageNodeInfo(busMessage);
            String key = MESSAGE_PREFIX + ":" + busMessage.getTopic();
            String message = messageNodeInfo.toString();
            // 完成playload信息base64编码
            if (config.isBase64()) {
                message = StrUtil.base64(messageNodeInfo);
            }
            // 是否加上随机Id
            if (!config.isSimple()) {
                message = StrUtil.addId(message);
            }
            RedisFuture<Long> future = ASYNC_COMMAND.zadd(key, messageNodeInfo.getCreateTime(), message);
            future.thenAccept(value -> {
            });
        });
    }

    @Override
    protected void destroyPlugin() {
        CONNECTION.close();
        CLIENT.shutdown();
    }
}
