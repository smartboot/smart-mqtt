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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.PluginConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * JWT认证器
 * 使用JWT Token进行认证，适用于无状态、微服务架构
 * 
 * 支持两种模式：
 * 1. 密码字段传递JWT Token
 * 2. 用户名字段传递JWT Token（password为secret验证）
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class JwtAuthenticator extends AbstractAuthenticator {
    
    private String secret;
    private String issuer;
    private List<String> audiences;
    private String usernameClaim = "sub";
    private boolean verifyUsername = true;
    private Algorithm algorithm;
    private JWTVerifier verifier;
    
    public JwtAuthenticator() {
        super("jwt");
    }
    
    @Override
    protected void doInitialize(PluginConfig.AuthenticatorConfig config) {
        this.secret = config.getStringOption("secret", null);
        this.issuer = config.getStringOption("issuer", null);
        this.usernameClaim = config.getStringOption("usernameClaim", "sub");
        this.verifyUsername = config.getBooleanOption("verifyUsername", true);
        
        // 可选的audience列表
        @SuppressWarnings("unchecked")
        List<String> audList = (List<String>) config.getOptions().get("audiences");
        if (audList != null && !audList.isEmpty()) {
            this.audiences = audList;
        }
        
        // 初始化JWT算法
        initializeAlgorithm();
    }
    
    private void initializeAlgorithm() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("JWT secret is required");
        }
        
        // 使用HMAC256算法
        algorithm = Algorithm.HMAC256(secret);
        
        // 构建验证器
        com.auth0.jwt.JWTVerifier.BaseVerification verification = (com.auth0.jwt.JWTVerifier.BaseVerification) JWT
                .require(algorithm)
                .acceptLeeway(5); // 5秒时间容差
        
        if (issuer != null && !issuer.isEmpty()) {
            verification.withIssuer(issuer);
        }
        
        verifier = verification.build();
    }
    
    @Override
    public AuthResult authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] passwordBytes = getPassword(message);
        
        if (passwordBytes == null || passwordBytes.length == 0) {
            return AuthResult.CONTINUE;
        }
        
        String token = new String(passwordBytes, StandardCharsets.UTF_8);
        
        try {
            DecodedJWT jwt = verifier.verify(token);
            
            // 验证用户名（如果启用）
            if (verifyUsername && username != null && !username.isEmpty()) {
                String tokenUsername = jwt.getClaim(usernameClaim).asString();
                if (tokenUsername == null || !tokenUsername.equals(username)) {
                    return AuthResult.FAILURE;
                }
            }
            
            // 验证audience（如果配置）
            if (audiences != null && !audiences.isEmpty()) {
                List<String> tokenAudiences = jwt.getAudience();
                boolean audienceValid = false;
                for (String aud : audiences) {
                    if (tokenAudiences.contains(aud)) {
                        audienceValid = true;
                        break;
                    }
                }
                if (!audienceValid) {
                    return AuthResult.FAILURE;
                }
            }
            
            return AuthResult.SUCCESS;
            
        } catch (JWTVerificationException e) {
            // Token验证失败
            return AuthResult.FAILURE;
        }
    }
}
