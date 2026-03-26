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

import tech.smartboot.redisun.Redisun;
import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.PluginConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.nio.charset.StandardCharsets;

/**
 * Redis认证器
 * 从Redis查询用户凭证进行认证，适用于分布式、高并发场景
 * 
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
    
    private String host = "localhost";
    private int port = 6379;
    private String redisPassword;
    private int database = 0;
    private String keyPrefix = "mqtt:auth:";
    private int connectionTimeout = 2000;
    
    private Redisun redisun;
    
    public RedisAuthenticator() {
        super("redis");
    }
    
    @Override
    protected void doInitialize(PluginConfig.AuthenticatorConfig config) {
        this.host = config.getStringOption("host", "localhost");
        this.port = config.getIntOption("port", 6379);
        this.redisPassword = config.getStringOption("password", null);
        this.database = config.getIntOption("database", 0);
        this.keyPrefix = config.getStringOption("keyPrefix", "mqtt:auth:");
        this.connectionTimeout = config.getIntOption("connectionTimeout", 2000);
        
        // 初始化 Redisun 客户端
        initializeRedisun();
    }
    
    /**
     * 初始化 Redisun 客户端
     */
    private void initializeRedisun() {
        // 构建连接地址
        StringBuilder addressBuilder = new StringBuilder("redis://");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            // 带认证的地址格式：redis://password@host:port
            addressBuilder.append(redisPassword).append("@");
        }
        addressBuilder.append(host).append(":").append(port);
        
        String address = addressBuilder.toString();
        
        // 创建 Redisun 实例
        redisun = Redisun.create(options -> {
            options.setAddress(address);
            options.setDatabase(database);
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
    public AuthResult authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] password = getPassword(message);
            
        if (username == null || username.isEmpty() || password == null || password.length == 0) {
            return AuthResult.CONTINUE;
        }
            
        String expectedPassword = null;
            
        try {
            // 从 Redis 获取密码
            expectedPassword = redisun.get(keyPrefix + username);
        } catch (Exception e) {
            // Redis 连接异常，返回 CONTINUE 让下一个认证器处理
            return AuthResult.CONTINUE;
        }
            
        if (expectedPassword == null) {
            // 用户不存在
            return AuthResult.CONTINUE;
        }
            
        if (verifyPassword(username, password, expectedPassword)) {
            return AuthResult.SUCCESS;
        }
            
        return AuthResult.FAILURE;
    }
    
    /**
     * 添加或更新用户（动态更新 Redis）
     */
    public void setUserPassword(String username, String password) {
        redisun.set(keyPrefix + username, password);
    }
    
    /**
     * 删除用户
     */
    public void removeUser(String username) {
        redisun.del(keyPrefix + username);
    }
}
