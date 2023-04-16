/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.provider;

import org.smartboot.http.restful.RestfulBootstrap;
import org.smartboot.mqtt.broker.provider.impl.message.MemoryPersistenceProvider;
import org.smartboot.mqtt.broker.provider.impl.session.MemorySessionStateProvider;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class Providers {
    private SessionStateProvider sessionStateProvider = new MemorySessionStateProvider();

    private PersistenceProvider retainMessageProvider = new MemoryPersistenceProvider();
    private PersistenceProvider persistenceProvider = new MemoryPersistenceProvider();

    private SubscribeProvider subscribeProvider = (topicFilter, session) -> true;

    /**
     * OpenAPI 处理器
     */
    private RestfulBootstrap openApiBootStrap;

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

    public SubscribeProvider getSubscribeProvider() {
        return subscribeProvider;
    }

    public void setSubscribeProvider(SubscribeProvider subscribeProvider) {
        this.subscribeProvider = subscribeProvider;
    }

    public RestfulBootstrap getOpenApiBootStrap() {
        return openApiBootStrap;
    }

    public void setOpenApiBootStrap(RestfulBootstrap openApiBootStrap) {
        this.openApiBootStrap = openApiBootStrap;
    }
}
