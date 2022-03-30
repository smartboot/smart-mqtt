package org.smartboot.mqtt.broker.store;

/**
 * 消息存储器
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public interface IMessagesStore {

    /**
     * 提取指定位置的数据
     *
     * @param offset
     * @return
     */
    StoredMessage poll(long offset);

    long latestOffset();

    /**
     * 清除指定topic的所有消息
     */
    void cleanTopic();

    /**
     * 存储消息
     */
    void storeTopic(StoredMessage storedMessage);
}
