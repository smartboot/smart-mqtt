package org.smartboot.socket.mqtt.spi;

import java.util.Collection;

/**
 * 消息存储器
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public interface IMessagesStore {

    /**
     * 初始化消息存储
     */
    void init();

    /**
     * Return a list of retained messages that satisfy the condition.
     *
     * @param condition
     *            the condition to match during the search.
     * @return the collection of matching messages.
     */
    Collection<StoredMessage> searchMatching(Topic key);

    void cleanRetained(Topic topic);

    void storeRetained(Topic topic, StoredMessage storedMessage);
}
