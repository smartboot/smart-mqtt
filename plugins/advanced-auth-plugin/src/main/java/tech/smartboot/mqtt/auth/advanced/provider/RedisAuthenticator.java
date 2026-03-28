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

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.PasswordEncoder;
import tech.smartboot.mqtt.auth.advanced.config.RedisConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.redisun.Redisun;

import java.nio.charset.StandardCharsets;
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
 * - connectionTimeout: 连接超时（默认 20000ms）
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class RedisAuthenticator extends AbstractAuthenticator {

    private Redisun redisun;
    private final RedisConfig config;
    private PasswordEncoder passwordEncoder;
    private static final String KEY_PREFIX = "smart-mqtt:auth:";

    public RedisAuthenticator(RedisConfig config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        // 创建 Redisun 实例
        redisun = Redisun.create(options -> {
            options.setAddress(config.getAddress())
                    .setDatabase(config.getDatabase())
                    .setPassword(config.getPassword());
        });

        // 初始化密码编码器
        String passwordEncoderName = config.getPasswordEncoder() != null ? config.getPasswordEncoder() : "plain";
        this.passwordEncoder = PasswordEncoder.getEncoder(passwordEncoderName);
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
        byte[] passwordBytes = getPassword(message);

        if (username == null || username.isEmpty() || passwordBytes == null || passwordBytes.length == 0) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        // Redis key
        String key = KEY_PREFIX + username;
        String rawPassword = new String(passwordBytes, StandardCharsets.UTF_8);

        // 异步从 Redis 获取密码哈希和盐值
        return redisun.asyncHmget(key, "password_hash", "salt").thenApply(values -> {
            String passwordHash = values.get(0);
            // 如果 Redis 中没有存储密码哈希，返回 CONTINUE 让下一个认证器处理
            if (passwordHash == null || passwordHash.isEmpty()) {
                return AuthResult.CONTINUE;
            }

            // 如果有盐值，将盐值作为密码前缀进行加密
            String salt = values.get(1);
            String saltPassword = FeatUtils.isBlank(salt) ? rawPassword : salt + rawPassword;
            // 使用配置的加密算法计算密码哈希
            String computedHash = passwordEncoder.encode(saltPassword);

            // 比较计算出的哈希值与 Redis 中存储的哈希值
            if (computedHash != null && computedHash.equalsIgnoreCase(passwordHash)) {
                return AuthResult.SUCCESS;
            } else {
                return AuthResult.FAILURE;
            }
        }).exceptionally(throwable -> {
            // Redis 连接异常，返回 CONTINUE 让下一个认证器处理
            return AuthResult.CONTINUE;
        });
    }

    @Override
    public String getName() {
        return AUTH_TYPE_REDIS;
    }
}
