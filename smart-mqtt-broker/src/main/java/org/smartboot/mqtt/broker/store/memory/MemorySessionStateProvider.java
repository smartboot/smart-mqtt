package org.smartboot.mqtt.broker.store.memory;

import org.smartboot.mqtt.broker.store.SessionState;
import org.smartboot.mqtt.broker.store.SessionStateProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public class MemorySessionStateProvider implements SessionStateProvider {
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    @Override
    public void store(String clientId, SessionState sessionState) {
        sessionStates.put(clientId, sessionState);
    }

    @Override
    public SessionState get(String clientId) {
        return sessionStates.get(clientId);
    }
}
