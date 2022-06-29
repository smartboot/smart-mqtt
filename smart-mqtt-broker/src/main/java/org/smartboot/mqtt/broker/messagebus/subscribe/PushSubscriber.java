package org.smartboot.mqtt.broker.messagebus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.messagebus.Message;

/**
 * 触发Topic的消息推送
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class PushSubscriber extends AbstractSubscriber {

    public PushSubscriber(BrokerContext brokerContext) {
        super(brokerContext);
    }

    @Override
    public void subscribe(Message message) {
        brokerContext.batchPublish(message.getTopic());
    }
}
