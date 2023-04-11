/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider.impl.session;

import org.smartboot.mqtt.broker.provider.SessionStateProvider;

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

    @Override
    public void remove(String clientId) {
        sessionStates.remove(clientId);
    }

}
