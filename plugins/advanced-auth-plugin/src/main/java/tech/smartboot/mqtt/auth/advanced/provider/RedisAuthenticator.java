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
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
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
                    .debug( true)
                    .setUsername(config.getUsername())
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

    public static void main(String[] args) {
        Redisun redisun = Redisun.create(options -> {
            options.setAddress("redis://127.0.0.1:6379")
                    .setDatabase(0);
        });
        
        // 1. 创建 base64 加密的账号
        PasswordEncoder base64Encoder = PasswordEncoder.getEncoder("base64");
        SecureRandom secureRandom = new SecureRandom();
        byte[] base64SaltBytes = new byte[16];
        secureRandom.nextBytes(base64SaltBytes);
        String base64Salt = bytesToHex(base64SaltBytes);
        Map<String, String> base64Values = new HashMap<>();
        String base64PasswordHash = base64Encoder.encode(base64Salt + "base64_pass");
        base64Values.put("password_hash", base64PasswordHash);
        base64Values.put("salt", base64Salt);
        redisun.hmset("smart-mqtt:auth:base64_user", base64Values);

        // 2. 创建 sha256 加密的账号
        PasswordEncoder sha256Encoder = PasswordEncoder.getEncoder("sha256");
        byte[] sha256SaltBytes = new byte[16];
        secureRandom.nextBytes(sha256SaltBytes);
        String sha256Salt = bytesToHex(sha256SaltBytes);
        Map<String, String> sha256Values = new HashMap<>();
        String sha256PasswordHash = sha256Encoder.encode(sha256Salt + "sha256_pass");
        sha256Values.put("password_hash", sha256PasswordHash);
        sha256Values.put("salt", sha256Salt);
        redisun.hmset("smart-mqtt:auth:sha256_user", sha256Values);

        // 3. 创建 plain 明文加密的账号（不推荐用于生产环境）
        PasswordEncoder plainEncoder = PasswordEncoder.getEncoder("plain");
        byte[] plainSaltBytes = new byte[16];
        secureRandom.nextBytes(plainSaltBytes);
        String plainSalt = bytesToHex(plainSaltBytes);
        Map<String, String> plainValues = new HashMap<>();
        String plainPasswordHash = plainEncoder.encode(plainSalt + "plain_pass");
        plainValues.put("password_hash", plainPasswordHash);
        plainValues.put("salt", plainSalt);
        redisun.hmset("smart-mqtt:auth:plain_user", plainValues);

        System.out.println("认证账号创建成功！");
        System.out.println("===========================================");
        System.out.println("Base64 加密账号：base64_user / base64_pass (盐值：" + base64Salt + ")");
        System.out.println("SHA256 加密账号：sha256_user / sha256_pass (盐值：" + sha256Salt + ")");
        System.out.println("Plain 明文账号：plain_user / plain_pass (盐值：" + plainSalt + ")");
        System.out.println("===========================================");
        System.out.println("\n注意：");
        System.out.println("- base64 和 sha256 提供了基本的安全性");
        System.out.println("- plain 为明文存储，仅用于测试，不建议在生产环境使用");
        System.out.println("- 所有账号均使用独立随机盐值，增强安全性");
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
