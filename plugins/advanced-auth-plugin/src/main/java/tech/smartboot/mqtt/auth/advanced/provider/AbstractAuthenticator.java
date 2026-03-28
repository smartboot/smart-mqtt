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

}
