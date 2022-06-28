package org.smartboot.mqtt.broker.eventbus.subscribe;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerTopic;
import org.smartboot.mqtt.broker.eventbus.EventMessage;
import org.smartboot.mqtt.common.AsyncTask;

/**
 * 消息Push触发器
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/25
 */
public class PushTriggerSubscriber extends AbstractSubscriber {
    public PushTriggerSubscriber(BrokerContext brokerContext) {
        super(brokerContext);

    }

    @Override
    public void subscribe(EventMessage eventMessage) {
        //触发该Topic的Push事件
        BrokerTopic topic = brokerContext.getOrCreateTopic(eventMessage.getTopic());
        topic.getConsumeOffsets().values().stream()
                .filter(consumeOffset -> consumeOffset.getSemaphore().availablePermits() > 0)
                .forEach(consumeOffset -> brokerContext.pushExecutorService().execute(new AsyncTask() {
                    @Override
                    public void execute() {
                        consumeOffset.getMqttSession().batchPublish(consumeOffset, brokerContext.pushExecutorService());
                    }
                }));
    }


}
