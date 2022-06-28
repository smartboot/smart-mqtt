package org.smartboot.mqtt.broker.eventbus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.Subscriber;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public abstract class AbstractSubscriber implements Subscriber {
    protected BrokerContext brokerContext;

    public AbstractSubscriber(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }
}
