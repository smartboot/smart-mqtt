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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文件认证器
 * 从文件加载用户凭证，支持自动刷新
 * 
 * 文件格式（每行一个用户）：
 * username:password
 * username2:password2
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class FileAuthenticator extends AbstractAuthenticator {
    
    private final Map<String, String> userStore = new ConcurrentHashMap<>();
    private String filePath;
    private boolean autoReload;
    private long reloadIntervalSeconds;
    private long lastModified;
    private ScheduledExecutorService reloadExecutor;
    
    public FileAuthenticator() {
        super("file");
    }
    
    @Override
    protected void doInitialize(PluginConfig.AuthenticatorConfig config) {
        this.filePath = config.getStringOption("path", "auth/users.conf");
        this.autoReload = config.getBooleanOption("autoReload", true);
        this.reloadIntervalSeconds = config.getIntOption("reloadInterval", 60);
        
        // 初始加载
        reload();
        
        // 启动定时刷新
        if (autoReload) {
            reloadExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FileAuth-Reload");
                t.setDaemon(true);
                return t;
            });
            reloadExecutor.scheduleWithFixedDelay(
                this::checkAndReload,
                reloadIntervalSeconds,
                reloadIntervalSeconds,
                TimeUnit.SECONDS
            );
        }
    }
    
    @Override
    public void destroy() {
        if (reloadExecutor != null) {
            reloadExecutor.shutdownNow();
        }
    }
    
    /**
     * 检查文件是否修改并重新加载
     */
    private void checkAndReload() {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return;
            }
            
            long currentModified = Files.getLastModifiedTime(path).toMillis();
            if (currentModified > lastModified) {
                reload();
            }
        } catch (IOException e) {
            // 忽略异常
        }
    }
    
    /**
     * 重新加载用户文件
     */
    public synchronized void reload() {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return;
        }
        
        Map<String, String> newStore = new ConcurrentHashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 解析用户名:密码
                int separatorIndex = line.indexOf(':');
                if (separatorIndex <= 0) {
                    continue;
                }
                
                String username = line.substring(0, separatorIndex).trim();
                String password = line.substring(separatorIndex + 1).trim();
                
                if (!username.isEmpty()) {
                    newStore.put(username, password);
                }
            }
            
            lastModified = file.lastModified();
            userStore.clear();
            userStore.putAll(newStore);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load auth file: " + filePath, e);
        }
    }
    
    @Override
    public AuthResult authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] password = getPassword(message);
        
        if (username == null || username.isEmpty() || password == null || password.length == 0) {
            return AuthResult.CONTINUE;
        }
        
        String expectedPassword = userStore.get(username);
        if (expectedPassword == null) {
            return AuthResult.CONTINUE;
        }
        
        if (verifyPassword(username, password, expectedPassword)) {
            return AuthResult.SUCCESS;
        }
        
        return AuthResult.FAILURE;
    }
    
    public int getUserCount() {
        return userStore.size();
    }
}
