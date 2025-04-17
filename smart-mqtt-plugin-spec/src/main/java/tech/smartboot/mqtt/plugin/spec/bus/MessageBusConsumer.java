/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.bus;


import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public interface MessageBusConsumer {
    /**
     * 消息消费
     * @param session   消息来源
     * @param message
     */
    void consume(MqttSession session, Message message);

    default boolean enable() {
        return true;
    }
}
