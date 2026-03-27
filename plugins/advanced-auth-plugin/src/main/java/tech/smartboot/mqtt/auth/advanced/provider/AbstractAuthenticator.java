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
import tech.smartboot.mqtt.auth.advanced.Authenticator;
import tech.smartboot.mqtt.auth.advanced.PasswordEncoder;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 认证器抽象基类
 * <p>
 * 提供统一的密码认证逻辑，子类只需实现数据获取方法
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public abstract class AbstractAuthenticator implements Authenticator {

    protected PasswordEncoder passwordEncoder;
    protected String passwordEncoderName = "plain";

    /**
     * 验证密码
     *
     * @param password         客户端提交的密码（字节数组）
     * @param expectedPassword 期望的编码密码
     * @return 是否匹配
     */
    protected boolean verifyPassword(byte[] password, String expectedPassword) {
        if (password == null || expectedPassword == null) {
            return false;
        }
        String rawPassword = new String(password, StandardCharsets.UTF_8);
        return passwordEncoder.matches(rawPassword, expectedPassword);
    }

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
     * 执行统一密码认证逻辑
     * <p>
     * 子类实现此方法获取期望密码，然后调用 doAuthenticate 完成认证
     *
     * @param session           会话对象
     * @param message           连接消息
     * @param expectedPassword  从存储（Redis/MySQL/HTTP响应）获取的期望密码
     * @return 认证结果
     */
    protected CompletableFuture<AuthResult> doAuthenticate(MqttSession session, MqttConnectMessage message, String expectedPassword) {
        // 匿名访问检查
        if (isAnonymous(message)) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        // 用户不存在
        if (expectedPassword == null) {
            return CompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        // 密码验证
        byte[] password = getPassword(message);
        if (verifyPassword(password, expectedPassword)) {
            return CompletableFuture.completedFuture(AuthResult.SUCCESS);
        }

        return CompletableFuture.completedFuture(AuthResult.FAILURE);
    }

    /**
     * 初始化密码编码器
     *
     * @param encoderName 编码器名称
     */
    protected void initPasswordEncoder(String encoderName) {
        this.passwordEncoderName = encoderName != null ? encoderName : "plain";
        this.passwordEncoder = PasswordEncoder.getEncoder(this.passwordEncoderName);
    }
}
