/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.bridge.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.bridge.redis.handler.BrokerHandler;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.PluginException;
<<<<<<< HEAD:smart-mqtt-data-persistence/src/main/java/org/smartboot/mqtt/data/persistence/impl/RedisPlugin.java
import org.smartboot.mqtt.data.persistence.DataPersistPlugin;
import org.smartboot.mqtt.data.persistence.config.imp.RedisPluginConfig;
import org.smartboot.mqtt.data.persistence.nodeinfo.MessageNodeInfo;
import org.smartboot.mqtt.data.persistence.utils.StrUtils;
=======
import org.smartboot.mqtt.common.eventbus.EventBus;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
>>>>>>> upgrade/master:plugins/redis-bridge-plugin/src/main/java/org/smartboot/mqtt/bridge/redis/RedisPlugin.java

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;


<<<<<<< HEAD:smart-mqtt-data-persistence/src/main/java/org/smartboot/mqtt/data/persistence/impl/RedisPlugin.java
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
=======
public class RedisPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['plugins']['redis-bridge'][0]";
    private static final String CRTEATE_TIME_FIELD_NAME = "createTime";
    private static final String RECENT_TIME_FIELD_NAME = "recentTime";
    private static final String DEFALUT_REDIS_BROKER_KEY_FIELD = "name";
    private static final Lock lock = new ReentrantLock();
    private static JedisPool jedisPool = null;

    private Config config;

    @Override
    protected void initPlugin(BrokerContext brokerContext) {
        config = brokerContext.parseConfig(CONFIG_JSON_PATH, Config.class);
>>>>>>> upgrade/master:plugins/redis-bridge-plugin/src/main/java/org/smartboot/mqtt/bridge/redis/RedisPlugin.java
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            throw new PluginException("start DataPersistRedisPlugin exception");
        }
<<<<<<< HEAD:smart-mqtt-data-persistence/src/main/java/org/smartboot/mqtt/data/persistence/impl/RedisPlugin.java
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
        messageBus.consumer((brokerContext1, publishMessage) -> {
            // 获得message信息Vo对象
            MessageNodeInfo messageNodeInfo = new MessageNodeInfo(publishMessage);
            String key = MESSAGE_PREFIX + brokerContext1.getBrokerConfigure().getName() + ":"
                    + publishMessage.getVariableHeader().getTopicName();
            String message = messageNodeInfo.toString();
            // 完成playload信息base64编码
            if (config.isBase64()){
                message = StrUtil.base64(messageNodeInfo);
            }
            // 是否加上随机Id
            if (!config.isSimple()){
                message = StrUtil.addId(message);
            }
            RedisFuture<Long> future = ASYNC_COMMAND.zadd(key, messageNodeInfo.getCreateTime(), message);
            future.thenAccept(value -> {});
        });
    }
    
=======

        // 完成redis线程池的
        if (jedisPool == null) {
            lock.lock();
            try {
                if (jedisPool == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMaxTotal(10);           // 最大连接数
                    poolConfig.setMaxIdle(2);              // 最大空闲连接数
                    poolConfig.setTestOnReturn(false);
                    poolConfig.setTestOnBorrow(true);       // 检查连接可用性, 确保获取的redis实例可用
                    poolConfig.setTestOnCreate(false);
                    jedisPool = new JedisPool(poolConfig, config.getHost(), config.getPort(), config.getTimeout(), config.getPassword());
                    LOGGER.info("redisPoll create success");
                }
            } finally {
                lock.unlock();
            }
        }
        EventBus eventBus = brokerContext.getEventBus();
        // 事件监听总线 监听broker创建
        eventBus.subscribe(ServerEventType.BROKER_STARTED, (eventType, subscriber) -> {
            Jedis resource = jedisPool.getResource();
            Map<String, String> handler = BrokerHandler.handler(brokerContext);
            String result = resource.hget(handler.get(DEFALUT_REDIS_BROKER_KEY_FIELD), CRTEATE_TIME_FIELD_NAME);
            if (result == null) {
                handler.put(CRTEATE_TIME_FIELD_NAME, handler.get(RECENT_TIME_FIELD_NAME));
            }
            resource.hmset(handler.get(DEFALUT_REDIS_BROKER_KEY_FIELD), handler);
            jedisPool.returnResource(resource);
        });

        // 消息总线监听
        MessageBus messageBus = brokerContext.getMessageBus();
//        messageBus.consumer((brokerContext1, publishMessage) -> {
//            Jedis resource = jedisPool.getResource();
//            String message = new MessageNodeInfo(publishMessage).toString(config.isBase64());
//            resource.lpush(brokerContext1.getBrokerConfigure().getName() + ":" + publishMessage.getVariableHeader().getTopicName(), message);
//            jedisPool.returnResource(resource);
//        });
    }


>>>>>>> upgrade/master:plugins/redis-bridge-plugin/src/main/java/org/smartboot/mqtt/bridge/redis/RedisPlugin.java
    @Override
    protected void destroyPlugin() {
        CONNECTION.close();
        CLIENT.shutdown();
    }
}
