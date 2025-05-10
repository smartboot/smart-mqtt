/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic.deliver;

/**
 * @author 三刀
 * @version v1.0 5/10/25
 */
public interface Push {
    /**
     * 将消息推送到客户端的抽象方法。
     * <p>
     * 具体的消息推送逻辑由子类实现，可能包括：
     * <ul>
     *   <li>消息的QoS处理</li>
     *   <li>消息的重传机制</li>
     *   <li>消息的确认机制</li>
     *   <li>消息的过滤规则</li>
     * </ul>
     * </p>
     */
    void pushToClient();
}
