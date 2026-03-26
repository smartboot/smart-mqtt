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

import org.yaml.snakeyaml.Yaml;
import tech.smartboot.mqtt.auth.advanced.provider.AbstractAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.FileAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.HttpAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.JwtAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.MemoryAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.RedisAuthenticator;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.AsyncEventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 高级认证插件
 * 
 * 特性：
 * 1. 支持认证链：多个认证器按优先级顺序执行
 * 2. 内置多种认证方式：内存、文件、HTTP、JWT、Redis
 * 3. 支持密码编码：明文、SHA256、Base64
 * 4. 支持匿名访问
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class AdvancedAuthPlugin extends Plugin {
    
    private AuthenticationChain authChain;
    private PluginConfig config;
    private BrokerContext brokerContext;
    
    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        this.brokerContext = brokerContext;
        
        log("==============================================");
        log("正在初始化高级认证插件...");
        log("==============================================");
        
        // 加载配置
        loadConfiguration();
        
        // 初始化认证链
        authChain = new AuthenticationChain(this, config);
        
        // 注册认证器
        registerAuthenticators();
        
        // 初始化所有认证器
        authChain.initializeAll();
        
        // 订阅CONNECT事件
        subscribe(EventType.CONNECT, AsyncEventObject.syncSubscriber((eventType, object) -> {
            MqttSession session = object.getSession();
            
            // 如果已经认证失败或断开，直接返回
            if (session.isDisconnect()) {
                return;
            }
            
            MqttConnectMessage message = object.getObject();
            
            // 检查是否匿名访问
            if (isAnonymous(message)) {
                if (config.isAllowAnonymous()) {
                    session.setAuthorized(true);
                    log("匿名访问已授权: " + session.getClientId());
                    return;
                } else {
                    log("匿名访问被拒绝: " + session.getClientId());
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                    return;
                }
            }
            
            // 执行认证链
            boolean success = authChain.doAuthenticate(session, message);
            
            if (success) {
                session.setAuthorized(true);
            } else {
                // 认证失败
                MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
            }
        }));
        
        log("==============================================");
        log("高级认证插件初始化完成");
        log("认证器数量: " + authChain.getAuthenticators().size());
        log("允许匿名访问: " + config.isAllowAnonymous());
        log("==============================================");
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfiguration() {
        try {
            File configFile = new File("plugins/advanced-auth-plugin.yaml");
            
            if (!configFile.exists()) {
                // 使用默认配置
                config = createDefaultConfig();
                log("配置文件不存在，使用默认配置");
                return;
            }
            
            try (InputStream is = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(is);
                
                config = new PluginConfig();
                
                if (map != null) {
                    if (map.containsKey("stopOnError")) {
                        config.setStopOnError((Boolean) map.get("stopOnError"));
                    }
                    if (map.containsKey("allowAnonymous")) {
                        config.setAllowAnonymous((Boolean) map.get("allowAnonymous"));
                    }
                    if (map.containsKey("authenticators")) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Map<String, Object>> authList = (java.util.List<Map<String, Object>>) map.get("authenticators");
                        if (authList != null) {
                            java.util.List<PluginConfig.AuthenticatorConfig> configs = new java.util.ArrayList<>();
                            for (Map<String, Object> authMap : authList) {
                                PluginConfig.AuthenticatorConfig ac = new PluginConfig.AuthenticatorConfig();
                                ac.setName(getString(authMap, "name"));
                                ac.setType(getString(authMap, "type"));
                                ac.setEnabled(getBoolean(authMap, "enabled", true));
                                ac.setOrder(getInt(authMap, "order", 100));
                                ac.setPasswordEncoder(getString(authMap, "passwordEncoder"));
                                if (ac.getPasswordEncoder() == null) {
                                    ac.setPasswordEncoder("plain");
                                }
                                if (authMap.containsKey("options")) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> opts = (Map<String, Object>) authMap.get("options");
                                    ac.setOptions(opts);
                                }
                                configs.add(ac);
                            }
                            config.setAuthenticators(configs);
                        }
                    }
                }
                
                log("配置文件加载成功");
            }
        } catch (Exception e) {
            log("加载配置文件失败: " + e.getMessage());
            config = createDefaultConfig();
        }
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
    
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        return val != null ? (Boolean) val : defaultVal;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    
    /**
     * 创建默认配置
     */
    private PluginConfig createDefaultConfig() {
        PluginConfig config = new PluginConfig();
        config.setStopOnError(true);
        config.setAllowAnonymous(false);
        
        java.util.List<PluginConfig.AuthenticatorConfig> authenticators = new java.util.ArrayList<>();
        
        // 默认文件认证器
        PluginConfig.AuthenticatorConfig fileAuth = new PluginConfig.AuthenticatorConfig();
        fileAuth.setName("file");
        fileAuth.setType("file");
        fileAuth.setOrder(100);
        fileAuth.setPasswordEncoder("plain");
        java.util.Map<String, Object> fileOpts = new java.util.HashMap<>();
        fileOpts.put("path", "auth/users.conf");
        fileOpts.put("autoReload", true);
        fileAuth.setOptions(fileOpts);
        authenticators.add(fileAuth);
        
        config.setAuthenticators(authenticators);
        
        return config;
    }
    
    /**
     * 注册所有认证器
     */
    private void registerAuthenticators() {
        if (config.getAuthenticators() == null || config.getAuthenticators().isEmpty()) {
            log("警告: 没有配置认证器");
            return;
        }
        
        for (PluginConfig.AuthenticatorConfig authConfig : config.getAuthenticators()) {
            if (!authConfig.isEnabled()) {
                log("跳过禁用的认证器: " + authConfig.getName());
                continue;
            }
            
            AbstractAuthenticator authenticator = createAuthenticator(authConfig.getType());
            if (authenticator != null) {
                authenticator.initialize(authConfig);
                authChain.registerAuthenticator(authenticator);
                log("注册认证器: " + authenticator.getName() + " (type=" + authConfig.getType() + ", order=" + authenticator.getOrder() + ")");
            } else {
                log("未知的认证器类型: " + authConfig.getType());
            }
        }
    }
    
    /**
     * 根据类型创建认证器
     */
    private AbstractAuthenticator createAuthenticator(String type) {
        if (type == null) {
            return null;
        }
        
        switch (type.toLowerCase()) {
            case "memory":
                return new MemoryAuthenticator();
            case "file":
                return new FileAuthenticator();
            case "http":
                return new HttpAuthenticator();
            case "jwt":
                return new JwtAuthenticator();
            case "redis":
                return new RedisAuthenticator();
            default:
                return null;
        }
    }
    
    @Override
    protected void destroyPlugin() {
        log("正在关闭高级认证插件...");
        
        if (authChain != null) {
            authChain.destroyAll();
        }
        
        log("高级认证插件已关闭");
    }
    
    @Override
    public String getVersion() {
        return Options.VERSION;
    }
    
    @Override
    public String getVendor() {
        return Options.VENDOR;
    }
    
    @Override
    public String pluginName() {
        return "advanced-auth-plugin";
    }
    
    /**
     * 检查是否为匿名连接
     */
    private boolean isAnonymous(MqttConnectMessage message) {
        String username = message.getPayload().userName();
        byte[] password = message.getPayload().passwordInBytes();
        return username == null || username.isEmpty() || password == null || password.length == 0;
    }
    
    /**
     * 获取认证链（供外部查询）
     */
    public AuthenticationChain getAuthChain() {
        return authChain;
    }
}
