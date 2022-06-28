package org.smartboot.mqtt.broker.messagebus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.messagebus.Message;

/**
 * 消息持久化并推送给订阅了该Topic的客户端
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class PersistenceAndPushSubscriber extends AbstractSubscriber {

    public PersistenceAndPushSubscriber(BrokerContext brokerContext) {
        super(brokerContext);
    }

    @Override
    public void subscribe(Message message) {
        brokerContext.getProviders().getPersistenceProvider().doSave(message);
        brokerContext.batchPublish(message.getTopic());
    }
}
