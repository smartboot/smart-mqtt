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
import tech.smartboot.mqtt.auth.advanced.PluginConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存认证器
 * 适用于配置固定账号或小型部署
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class MemoryAuthenticator extends AbstractAuthenticator {
    
    private final Map<String, String> userStore = new ConcurrentHashMap<>();
    
    public MemoryAuthenticator() {
        super("memory");
    }
    
    @Override
    protected void doInitialize(PluginConfig.AuthenticatorConfig config) {
        // 从配置加载用户
        @SuppressWarnings("unchecked")
        Map<String, Object> users = (Map<String, Object>) config.getOptions().get("users");
        if (users != null) {
            for (Map.Entry<String, Object> entry : users.entrySet()) {
                String username = entry.getKey();
                String password = entry.getValue() != null ? entry.getValue().toString() : "";
                userStore.put(username, password);
            }
        }
    }
    
    @Override
    public AuthResult authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] password = getPassword(message);
        
        // 检查是否为空
        if (username == null || username.isEmpty() || password == null || password.length == 0) {
            return AuthResult.CONTINUE;
        }
        
        // 验证用户
        String expectedPassword = userStore.get(username);
        if (expectedPassword == null) {
            // 用户不存在，让下一个认证器处理
            return AuthResult.CONTINUE;
        }
        
        if (verifyPassword(username, password, expectedPassword)) {
            return AuthResult.SUCCESS;
        }
        
        // 密码错误
        return AuthResult.FAILURE;
    }
    
    /**
     * 动态添加用户（支持运行时更新）
     */
    public void addUser(String username, String password) {
        userStore.put(username, password);
    }
    
    /**
     * 删除用户
     */
    public void removeUser(String username) {
        userStore.remove(username);
    }
    
    /**
     * 获取用户数
     */
    public int getUserCount() {
        return userStore.size();
    }
}
