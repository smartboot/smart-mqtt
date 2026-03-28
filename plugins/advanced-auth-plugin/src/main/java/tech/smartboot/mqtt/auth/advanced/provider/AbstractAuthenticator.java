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

import tech.smartboot.mqtt.auth.advanced.Authenticator;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 认证器抽象基类
 * <p>
 * 提供统一的密码认证逻辑，子类只需实现数据获取方法
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public abstract class AbstractAuthenticator implements Authenticator {

    /**
     * 获取连接的用户名
     */
    protected String getUsername(MqttConnectMessage message) {
        return message.getPayload().userName();
    }

    /**
     * 获取连接的密码
     */
    protected byte[] getPassword(MqttConnectMessage message) {
        return message.getPayload().passwordInBytes();
    }

    /**
     * 判断是否为匿名连接
     */
    protected boolean isAnonymous(MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] password = getPassword(message);
        return username == null || username.isEmpty() || password == null || password.length == 0;
    }

    /**
     * 对密码进行编码
     *
     * @param rawPassword 明文密码
     * @param encoderType 编码器类型（plain/sha256/base64/md5）
     * @return 编码后的密码
     */
    protected String encodePassword(String rawPassword, String encoderType) {
        if (rawPassword == null) {
            return null;
        }
        
        if (encoderType == null || encoderType.isEmpty() || "plain".equalsIgnoreCase(encoderType)) {
            return rawPassword;
        }
        
        if ("sha256".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }
        
        if ("base64".equalsIgnoreCase(encoderType)) {
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        }
        
        if ("md5".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }
        
        // 默认返回明文
        return rawPassword;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
