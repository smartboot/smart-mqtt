package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.persistence.message.MemoryPersistenceProvider;
import org.smartboot.mqtt.broker.persistence.session.MemorySessionStateProvider;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class Providers {
    private SessionStateProvider sessionStateProvider = new MemorySessionStateProvider();

    private PersistenceProvider retainMessageProvider = new MemoryPersistenceProvider();
    private PersistenceProvider persistenceProvider = new MemoryPersistenceProvider();

    private ConnectAuthenticationProvider connectAuthenticationProvider;

    public SessionStateProvider getSessionStateProvider() {
        return sessionStateProvider;
    }

    public void setSessionStateProvider(SessionStateProvider sessionStateProvider) {
        this.sessionStateProvider = sessionStateProvider;
    }

    public PersistenceProvider getRetainMessageProvider() {
        return retainMessageProvider;
    }

    public void setRetainMessageProvider(PersistenceProvider retainMessageProvider) {
        this.retainMessageProvider = retainMessageProvider;
    }

    public PersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
        this.persistenceProvider = persistenceProvider;
    }

    public ConnectAuthenticationProvider getConnectAuthenticationProvider() {
        return connectAuthenticationProvider;
    }

    public void setConnectAuthenticationProvider(ConnectAuthenticationProvider connectAuthenticationProvider) {
        this.connectAuthenticationProvider = connectAuthenticationProvider;
    }
}
