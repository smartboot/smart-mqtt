package org.smartboot.mqtt.broker.eventbus.messagebus.consumer;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.Message;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public abstract class AbstractConsumer implements Consumer<Message> {
    protected BrokerContext brokerContext;

    public AbstractConsumer(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

}
