package org.smartboot.mqtt.broker.store;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public interface SessionStateProvider {
    void store(String clientId, SessionState sessionState);

    SessionState get(String clientId);
}
