/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec.provider;


/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class Providers {
    private SessionStateProvider sessionStateProvider = new SessionStateProvider() {
        @Override
        public void store(String clientId, SessionState sessionState) {
            
        }

        @Override
        public SessionState get(String clientId) {
            return null;
        }

        @Override
        public void remove(String clientId) {

        }
    };

    private SubscribeProvider subscribeProvider = new SubscribeProvider() {
    };

    public SessionStateProvider getSessionStateProvider() {
        return sessionStateProvider;
    }

    public void setSessionStateProvider(SessionStateProvider sessionStateProvider) {
        this.sessionStateProvider = sessionStateProvider;
    }


    public SubscribeProvider getSubscribeProvider() {
        return subscribeProvider;
    }

    public void setSubscribeProvider(SubscribeProvider subscribeProvider) {
        this.subscribeProvider = subscribeProvider;
    }

}
