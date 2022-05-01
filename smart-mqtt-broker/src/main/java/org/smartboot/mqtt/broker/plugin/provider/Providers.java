package org.smartboot.mqtt.broker.plugin.provider;

import org.smartboot.mqtt.broker.plugin.provider.mock.MockClientAuthorizeProvider;
import org.smartboot.mqtt.broker.plugin.provider.mock.MockRetainMessageProvider;
import org.smartboot.mqtt.broker.plugin.provider.mock.MockTopicFilterProvider;
import org.smartboot.mqtt.broker.store.SessionStateProvider;
import org.smartboot.mqtt.broker.store.memory.MemoryMessageStoreProvider;
import org.smartboot.mqtt.broker.store.memory.MemorySessionStateProvider;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class Providers {
    private TopicFilterProvider topicFilterProvider = new MockTopicFilterProvider();
    private ClientAuthorizeProvider clientAuthorizeProvider = new MockClientAuthorizeProvider();
    private MessageStoreProvider messageStoreProvider = new MemoryMessageStoreProvider();
    private SessionStateProvider sessionStateProvider = new MemorySessionStateProvider();

    private RetainMessageProvider retainMessageProvider = new MockRetainMessageProvider();

    public TopicFilterProvider getTopicFilterProvider() {
        return topicFilterProvider;
    }

    public void setTopicFilterProvider(TopicFilterProvider topicFilterProvider) {
        this.topicFilterProvider = topicFilterProvider;
    }

    public ClientAuthorizeProvider getClientAuthorizeProvider() {
        return clientAuthorizeProvider;
    }

    public void setClientAuthorizeProvider(ClientAuthorizeProvider clientAuthorizeProvider) {
        this.clientAuthorizeProvider = clientAuthorizeProvider;
    }

    public MessageStoreProvider getMessageStoreProvider() {
        return messageStoreProvider;
    }

    public void setMessageStoreProvider(MessageStoreProvider messageStoreProvider) {
        this.messageStoreProvider = messageStoreProvider;
    }

    public SessionStateProvider getSessionStateProvider() {
        return sessionStateProvider;
    }

    public void setSessionStateProvider(SessionStateProvider sessionStateProvider) {
        this.sessionStateProvider = sessionStateProvider;
    }

    public RetainMessageProvider getRetainMessageProvider() {
        return retainMessageProvider;
    }

    public void setRetainMessageProvider(RetainMessageProvider retainMessageProvider) {
        this.retainMessageProvider = retainMessageProvider;
    }
}
