package org.smartboot.socket.mqtt.spi.impl;

import org.smartboot.socket.mqtt.spi.IMessagesStore;
import org.smartboot.socket.mqtt.spi.StoredMessage;
import org.smartboot.socket.mqtt.spi.Topic;

import java.util.Collection;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class MemoryMessageStore implements IMessagesStore{
    @Override
    public void init() {

    }

    @Override
    public Collection<StoredMessage> searchMatching(Topic key) {
        return null;
    }

    @Override
    public void cleanRetained(Topic topic) {

    }

    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {

    }
}
