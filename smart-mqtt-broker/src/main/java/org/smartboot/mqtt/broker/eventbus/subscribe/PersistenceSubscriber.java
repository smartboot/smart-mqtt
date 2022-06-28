package org.smartboot.mqtt.broker.eventbus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.EventMessage;

/**
 * 消息持久化
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class PersistenceSubscriber extends AbstractSubscriber {

    public PersistenceSubscriber(BrokerContext brokerContext) {
        super(brokerContext);
    }

    @Override
    public void subscribe(EventMessage message) {
        System.out.println("sink...");
        brokerContext.getProviders().getPersistenceProvider().doSave(message);
    }
}
