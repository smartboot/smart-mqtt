package org.smartboot.mqtt.broker;

/**
 * @author qinluo
 * @date 2022-08-05 16:42:40
 * @since 1.0.0
 */
public interface AuthenticationService {

    /**
     * 进行用户名密码授权认证
     *
     * @param username 用户名
     * @param password 密码
     * @param session  当前连接绘画
     * @return         是否认证成功
     */
    boolean authentication(String username, String password, MqttSession session);
}
