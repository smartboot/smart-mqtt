/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec.provider;

import org.smartboot.mqtt.common.MqttSession;
import org.smartboot.mqtt.plugin.spec.BrokerTopic;

/**
 * MQTT主题订阅提供者接口，用于处理客户端的主题订阅请求和主题匹配逻辑。
 * <p>
 * 该接口主要负责：
 * <ul>
 *   <li>验证客户端订阅的主题过滤器是否合法</li>
 *   <li>处理特殊主题（如以$开头的主题）的订阅规则</li>
 *   <li>支持共享订阅（$share/）功能</li>
 *   <li>控制客户端对系统主题（$SYS/）的访问权限</li>
 * </ul>
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/28
 */
public interface SubscribeProvider {
    /**
     * 验证客户端是否可以订阅指定的主题过滤器。
     * <p>
     * 根据MQTT协议规范4.7.2节，普通应用不能订阅以$字符开头的主题，但允许订阅共享主题（以$share/开头）。
     *
     * @param topicFilter 客户端请求订阅的主题过滤器
     * @param session 发起订阅请求的MQTT会话
     * @return 如果允许订阅返回true，否则返回false
     */
    default boolean subscribeTopic(String topicFilter, MqttSession session) {
        //4.7.2 应用不能使用 $ 字符开头的主题
        return !topicFilter.startsWith("$") || topicFilter.startsWith("$share/");
    }

    /**
     * 判断指定的会话是否可以接收某个主题的消息。
     * <p>
     * 用于控制客户端对系统主题（$SYS/）的访问权限，默认实现禁止客户端接收系统主题的消息。
     *
     * @param brokerTopic 待匹配的主题
     * @param session 需要判断权限的会话
     * @return 如果允许接收消息返回true，否则返回false
     */
    default boolean matchTopic(BrokerTopic brokerTopic, MqttSession session) {
        return !brokerTopic.getTopic().startsWith("$SYS/");
    }

}
