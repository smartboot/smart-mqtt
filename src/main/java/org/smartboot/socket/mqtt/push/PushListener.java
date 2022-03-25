package org.smartboot.socket.mqtt.push;

import org.smartboot.socket.mqtt.common.Topic;
import org.smartboot.socket.mqtt.store.SubscriberConsumeOffset;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/24
 */
public interface PushListener {
    void notify(Topic topic);

    void notify(SubscriberConsumeOffset consumeOffset);
}
