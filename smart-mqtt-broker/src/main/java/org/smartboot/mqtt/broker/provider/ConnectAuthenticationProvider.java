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
