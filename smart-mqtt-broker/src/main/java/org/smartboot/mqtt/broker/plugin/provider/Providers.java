package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.persistence.message.MemoryPersistenceProvider;
import org.smartboot.mqtt.broker.persistence.message.PersistenceProvider;
import org.smartboot.mqtt.broker.persistence.session.MemorySessionStateProvider;
import org.smartboot.mqtt.broker.persistence.session.SessionStateProvider;
import org.smartboot.mqtt.broker.plugin.provider.mock.MockClientAuthorizeProvider;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class Providers {
    private ClientAuthorizeProvider clientAuthorizeProvider = new MockClientAuthorizeProvider();
    private SessionStateProvider sessionStateProvider = new MemorySessionStateProvider();

    private PersistenceProvider retainMessageProvider = new MemoryPersistenceProvider();
    private PersistenceProvider persistenceProvider = new MemoryPersistenceProvider();

    public ClientAuthorizeProvider getClientAuthorizeProvider() {
        return clientAuthorizeProvider;
    }

    public void setClientAuthorizeProvider(ClientAuthorizeProvider clientAuthorizeProvider) {
        this.clientAuthorizeProvider = clientAuthorizeProvider;
    }


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

}
