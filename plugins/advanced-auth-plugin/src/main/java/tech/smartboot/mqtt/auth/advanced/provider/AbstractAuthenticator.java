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
import tech.smartboot.mqtt.auth.advanced.PasswordEncoder;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;

import java.nio.charset.StandardCharsets;

/**
 * 认证器抽象基类
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public abstract class AbstractAuthenticator implements Authenticator {

    protected PasswordEncoder passwordEncoder;

    /**
     * 验证用户名密码
     *
     * @param username         用户名
     * @param password         密码（字节数组）
     * @param expectedPassword 期望的编码密码
     * @return 是否匹配
     */
    protected boolean verifyPassword(String username, byte[] password, String expectedPassword) {
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
}
