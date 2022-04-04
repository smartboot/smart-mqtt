package org.smartboot.mqtt.broker.store;

import org.smartboot.mqtt.common.StoredMessage;

import java.util.function.Consumer;

/**
 * 消息存储器
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public interface MessageQueue {

    /**
     * 遍历消息队列中的数据
     */
    void forEach(Consumer<StoredMessage> consumer);

    /**
     * 存储消息
     */
    void put(StoredMessage storedMessage);
}
