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

import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

/**
 * 认证器接口
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public interface Authenticator {
    
    /**
     * 执行认证
     * 
     * @param session 会话对象
     * @param message 连接消息
     * @return 认证结果
     */
    AuthResult authenticate(MqttSession session, MqttConnectMessage message);
    
    /**
     * 获取认证器名称
     * 
     * @return 认证器标识名称
     */
    String getName();
    
    /**
     * 获取认证器优先级，数值越小优先级越高
     * 
     * @return 优先级
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * 是否启用该认证器
     * 
     * @return true表示启用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 初始化认证器
     * 
     * @param config 配置对象
     */
    default void initialize(PluginConfig.AuthenticatorConfig config) {
    }
    
    /**
     * 销毁认证器
     */
    default void destroy() {
    }
}
