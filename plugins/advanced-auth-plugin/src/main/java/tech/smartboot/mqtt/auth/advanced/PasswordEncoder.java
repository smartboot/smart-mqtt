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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 密码编码器接口及实现
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public interface PasswordEncoder {
    
    /**
     * 编码明文密码
     * 
     * @param rawPassword 明文密码
     * @return 编码后的密码
     */
    String encode(String rawPassword);
    
    /**
     * 验证密码是否匹配
     * 
     * @param rawPassword 明文密码
     * @param encodedPassword 编码后的密码
     * @return true表示匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
    
    /**
     * 获取编码器名称
     * 
     * @return 编码器名称
     */
    String getName();
    
    /**
     * 工厂方法：根据名称获取编码器
     */
    static PasswordEncoder getEncoder(String name) {
        if (name == null || name.isEmpty() || "plain".equalsIgnoreCase(name)) {
            return new PlainPasswordEncoder();
        }
        if ("sha256".equalsIgnoreCase(name)) {
            return new Sha256PasswordEncoder();
        }
        if ("base64".equalsIgnoreCase(name)) {
            return new Base64PasswordEncoder();
        }
        return new PlainPasswordEncoder();
    }
    
    /**
     * 明文密码编码器
     */
    class PlainPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            return rawPassword;
        }
        
        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            if (rawPassword == null || encodedPassword == null) {
                return false;
            }
            return rawPassword.equals(encodedPassword);
        }
        
        @Override
        public String getName() {
            return "plain";
        }
    }
    
    /**
     * SHA-256 密码编码器
     */
    class Sha256PasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            if (rawPassword == null) {
                return null;
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }
        
        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            if (rawPassword == null || encodedPassword == null) {
                return false;
            }
            return encode(rawPassword).equalsIgnoreCase(encodedPassword);
        }
        
        @Override
        public String getName() {
            return "sha256";
        }
        
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
    
    /**
     * Base64 密码编码器
     */
    class Base64PasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            if (rawPassword == null) {
                return null;
            }
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            if (rawPassword == null || encodedPassword == null) {
                return false;
            }
            return encode(rawPassword).equals(encodedPassword);
        }
        
        @Override
        public String getName() {
            return "base64";
        }
    }
}
