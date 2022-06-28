package org.smartboot.mqtt.broker.eventbus;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public interface EventFilter {

    boolean accept(EventMessage message);
}
