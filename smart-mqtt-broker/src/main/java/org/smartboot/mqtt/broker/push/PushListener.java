package org.smartboot.mqtt.broker.push;

import org.smartboot.mqtt.broker.Topic;
import org.smartboot.mqtt.broker.store.SubscriberConsumeOffset;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/24
 */
public interface PushListener {
    void notify(Topic topic);

    void notify(SubscriberConsumeOffset consumeOffset);
}
