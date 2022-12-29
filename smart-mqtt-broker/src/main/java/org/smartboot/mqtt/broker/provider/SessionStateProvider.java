package org.smartboot.mqtt.broker.provider;

import org.smartboot.mqtt.broker.provider.impl.session.SessionState;

/**
 * 会话状态Provider
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public interface SessionStateProvider {
    /**
     * 存储会话状态
     */
    void store(String clientId, SessionState sessionState);

    /**
     * 获取指定clientId的会话状态
     */
    SessionState get(String clientId);

    /**
     * 清理会话状态
     */
    void remove(String clientId);
}
