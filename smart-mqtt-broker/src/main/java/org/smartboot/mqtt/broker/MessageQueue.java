/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.broker.eventbus.messagebus.Message;

public interface MessageQueue {
    void put(Message message);

    Message get(long offset);

    //提交指定offset的消息
    void commit(long offset);

    long getLatestOffset();

    /**
     * 清空消息队列
     */
    void clear();

    //消息队列容量
    int capacity();
}
