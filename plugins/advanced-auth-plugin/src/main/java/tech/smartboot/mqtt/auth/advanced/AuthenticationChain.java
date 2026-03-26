/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced;

import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 认证链管理器
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class AuthenticationChain {
    
    private final List<Authenticator> authenticators = new ArrayList<>();
    private final Map<String, Authenticator> authenticatorMap = new ConcurrentHashMap<>();
    private final PluginConfig config;
    private final AdvancedAuthPlugin plugin;
    
    public AuthenticationChain(AdvancedAuthPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /**
     * 注册认证器
     */
    public void registerAuthenticator(Authenticator authenticator) {
        if (authenticatorMap.putIfAbsent(authenticator.getName(), authenticator) == null) {
            authenticators.add(authenticator);
            // 按优先级排序
            authenticators.sort(Comparator.comparingInt(Authenticator::getOrder));
        }
    }
    
    /**
     * 执行认证链
     * 
     * @param session 会话对象
     * @param message 连接消息
     * @return true表示认证成功
     */
    public boolean doAuthenticate(MqttSession session, MqttConnectMessage message) {
        String clientId = session.getClientId();
        String username = message.getPayload().userName();
        
        // 获取启用的认证器
        List<Authenticator> enabledAuthenticators = authenticators.stream()
                .filter(Authenticator::isEnabled)
                .collect(Collectors.toList());
        
        if (enabledAuthenticators.isEmpty()) {
            plugin.log("警告: 没有启用的认证器，默认拒绝连接");
            return false;
        }
        
        for (Authenticator authenticator : enabledAuthenticators) {
            try {
                plugin.log("尝试认证: " + authenticator.getName() + " - clientId=" + clientId + ", username=" + username);
                AuthResult result = authenticator.authenticate(session, message);
                
                switch (result) {
                    case SUCCESS:
                        plugin.log("认证成功: " + authenticator.getName() + " - clientId=" + clientId);
                        return true;
                    case FAILURE:
                        plugin.log("认证失败: " + authenticator.getName() + " - clientId=" + clientId);
                        return false;
                    case CONTINUE:
                        // 继续下一个认证器
                        continue;
                    case CONTINUE_AUTH:
                        // MQTT 5.0 增强认证 - 暂不支持
                        plugin.log("增强认证暂未支持: " + authenticator.getName());
                        continue;
                }
            } catch (Exception e) {
                plugin.log("认证器异常: " + authenticator.getName() + ", error=" + e.getMessage());
                if (config.isStopOnError()) {
                    return false;
                }
            }
        }
        
        // 所有认证器都返回CONTINUE，默认拒绝
        plugin.log("认证失败: 没有认证器能处理该请求 - clientId=" + clientId);
        return false;
    }
    
    /**
     * 初始化所有认证器
     */
    public void initializeAll() {
        for (Authenticator authenticator : authenticators) {
            try {
                PluginConfig.AuthenticatorConfig authConfig = config.getAuthenticatorConfig(authenticator.getName());
                if (authConfig != null) {
                    authenticator.initialize(authConfig);
                }
                plugin.log("初始化认证器成功: " + authenticator.getName());
            } catch (Exception e) {
                plugin.log("初始化认证器失败: " + authenticator.getName() + ", error=" + e.getMessage());
            }
        }
    }
    
    /**
     * 销毁所有认证器
     */
    public void destroyAll() {
        for (Authenticator authenticator : authenticators) {
            try {
                authenticator.destroy();
            } catch (Exception e) {
                plugin.log("销毁认证器异常: " + authenticator.getName() + ", error=" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取认证器列表
     */
    public List<Authenticator> getAuthenticators() {
        return new ArrayList<>(authenticators);
    }
}
