/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced.provider;

import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.config.PluginConfig;
import tech.smartboot.mqtt.auth.advanced.config.RedisConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.redisun.Redisun;

import java.util.concurrent.CompletableFuture;

/**
 * Redis认证器
 * 从Redis查询用户凭证进行认证，适用于分布式、高并发场景
 * <p>
 * 配置选项：
 * - host: Redis主机（默认localhost）
 * - port: Redis端口（默认6379）
 * - password: Redis密码
 * - database: 数据库索引（默认0）
 * - keyPrefix: 用户 key 前缀（默认 mqtt:auth:）
 * - connectionTimeout: 连接超时（默认 2000ms）
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class RedisAuthenticator extends AbstractAuthenticator {

    private Redisun redisun;
    private final RedisConfig config;

    public RedisAuthenticator(RedisConfig config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        // 创建 Redisun 实例
        redisun = Redisun.create(options -> {
            options.setAddress(config.getAddress())
                    .setDatabase(config.getDatabase()).setPassword(config.getPassword());
//            options.setConnectTimeout(connectionTimeout);
        });
    }


    @Override
    public void destroy() {
        if (redisun != null) {
            redisun.close();
        }
    }

    @Override
    public CompletableFuture<AuthResult> authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] password = getPassword(message);

        if (username == null || username.isEmpty() || password == null || password.length == 0) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        String expectedPassword = null;

        try {
            // 从 Redis 获取密码
            expectedPassword = redisun.get(config.getKeyPrefix() + username);
        } catch (Exception e) {
            // Redis 连接异常，返回 CONTINUE 让下一个认证器处理
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        if (expectedPassword == null) {
            // 用户不存在
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        if (verifyPassword(username, password, expectedPassword)) {
            return CompletableFuture.completedFuture(AuthResult.SUCCESS);
        }

        return CompletableFuture.completedFuture(AuthResult.FAILURE);
    }

    @Override
    public String getName() {
        return AUTH_TYPE_REDIS;
    }
}
