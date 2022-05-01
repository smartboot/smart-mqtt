package org.smartboot.mqtt.broker.store;

import org.smartboot.mqtt.common.StoredMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

/**
 * 消息存储器
 *
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public interface MessageQueue {

    /**
     * 存储消息
     */
    StoredMessage put(String clientId, MqttPublishMessage message);

    /**
     * 获取指定位置的消息
     *
     * @param offset
     * @return
     */
    StoredMessage get(long offset);

    long getLatestOffset();
}
