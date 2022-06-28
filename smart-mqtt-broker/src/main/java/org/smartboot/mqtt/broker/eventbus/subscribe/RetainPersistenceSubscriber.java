package org.smartboot.mqtt.broker.eventbus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.EventMessage;
import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * Retain消息持久化
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class RetainPersistenceSubscriber extends AbstractSubscriber {
    public RetainPersistenceSubscriber(BrokerContext brokerContext) {
        super(brokerContext);
    }

    @Override
    public void subscribe(EventMessage message) {
        /*
         * 如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它必须丢弃之前为那个主题保留
         * 的任何消息。它应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都可以选择丢弃它
         * 如果这种情况发生了，那个主题将没有保留消息
         */
        if (message.isRetained()) {
            if (message.getMqttQoS() == MqttQoS.AT_MOST_ONCE) {
                brokerContext.getProviders().getRetainMessageProvider().delete(message.getTopic());
            }
            brokerContext.getProviders().getRetainMessageProvider().doSave(message);
        }
    }
}
