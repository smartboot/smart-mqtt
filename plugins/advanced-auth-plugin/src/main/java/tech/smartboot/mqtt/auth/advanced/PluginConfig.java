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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件配置类
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class PluginConfig {
    
    /**
     * 认证器出错时是否停止（默认true）
     */
    private boolean stopOnError = true;
    
    /**
     * 是否启用匿名访问（默认false）
     */
    private boolean allowAnonymous = false;
    
    /**
     * 认证器配置列表
     */
    private List<AuthenticatorConfig> authenticators;
    
    /**
     * 认证器配置映射（用于快速查找）
     */
    private Map<String, AuthenticatorConfig> authenticatorMap;
    
    public boolean isStopOnError() {
        return stopOnError;
    }
    
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }
    
    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }
    
    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }
    
    public List<AuthenticatorConfig> getAuthenticators() {
        return authenticators;
    }
    
    public void setAuthenticators(List<AuthenticatorConfig> authenticators) {
        this.authenticators = authenticators;
        // 构建映射
        this.authenticatorMap = new HashMap<>();
        if (authenticators != null) {
            for (AuthenticatorConfig config : authenticators) {
                authenticatorMap.put(config.getName(), config);
            }
        }
    }
    
    public AuthenticatorConfig getAuthenticatorConfig(String name) {
        return authenticatorMap != null ? authenticatorMap.get(name) : null;
    }
    
    /**
     * 认证器配置
     */
    public static class AuthenticatorConfig {
        /**
         * 认证器名称（唯一标识）
         */
        private String name;
        
        /**
         * 认证器类型：memory, file, http, jwt, redis
         */
        private String type;
        
        /**
         * 是否启用（默认true）
         */
        private boolean enabled = true;
        
        /**
         * 优先级（数值越小越优先，默认100）
         */
        private int order = 100;
        
        /**
         * 密码编码方式：plain, sha256, base64
         */
        private String passwordEncoder = "plain";
        
        /**
         * 额外配置参数
         */
        private Map<String, Object> options = new HashMap<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getOrder() {
            return order;
        }
        
        public void setOrder(int order) {
            this.order = order;
        }
        
        public String getPasswordEncoder() {
            return passwordEncoder;
        }
        
        public void setPasswordEncoder(String passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }
        
        public Map<String, Object> getOptions() {
            return options;
        }
        
        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
        
        /**
         * 获取字符串选项
         */
        public String getStringOption(String key, String defaultValue) {
            if (options == null || !options.containsKey(key)) {
                return defaultValue;
            }
            Object value = options.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        
        /**
         * 获取整数选项
         */
        public int getIntOption(String key, int defaultValue) {
            if (options == null || !options.containsKey(key)) {
                return defaultValue;
            }
            Object value = options.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        /**
         * 获取布尔选项
         */
        public boolean getBooleanOption(String key, boolean defaultValue) {
            if (options == null || !options.containsKey(key)) {
                return defaultValue;
            }
            Object value = options.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(value.toString());
        }
    }
}
