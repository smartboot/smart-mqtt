/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

/**
 * 连接认证
 * @author qinluo
 * @date 2022-08-05 16:42:40
 * @since 1.0.0
 */
public interface ConnectAuthenticationProvider {

    /**
     * 进行用户名密码授权认证
     *
     * @param connectMessage  connect消息
     * @param session  当前连接绘画
     * @return 是否认证成功
     */
    boolean authentication(MqttConnectMessage connectMessage, MqttSession session);
}
