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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.bridge.redis.handler.BrokerHandler;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.broker.plugin.PluginException;
import org.smartboot.mqtt.common.eventbus.EventBus;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


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
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            throw new PluginException("start DataPersistRedisPlugin exception");
        }

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


    @Override
    protected void destroyPlugin() {
        lock.lock();
        try {
            if (jedisPool != null) {
                jedisPool.close();
                // 防止内存泄漏
                jedisPool = null;
            }
        } finally {
            lock.unlock();
        }
    }
}
