package org.smartboot.mqtt.broker.plugin.provider;

/**
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public interface ClientAuthorizeProvider {
    /*
     * 客户端连接鉴权
     */
    boolean auth(String userName, String clientIdentifier, byte[] password);
}
