package org.smartboot.socket.mqtt.push.impl;

import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.push.PushListener;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.store.StoredMessage;
import org.smartboot.socket.mqtt.common.Topic;
import org.smartboot.socket.mqtt.store.SubscriberConsumeOffset;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/24
 */
public class PushListenerImpl implements PushListener {
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void notify(Topic topic) {
        ConcurrentLinkedQueue<SubscriberConsumeOffset> queue = topic.getConsumerGroup().getIdleSubscribers();
        SubscriberConsumeOffset subscriberConsumeOffset;
        while ((subscriberConsumeOffset = queue.poll()) != null) {
            if (subscriberConsumeOffset.isEnable()) {
                notify(subscriberConsumeOffset);
            }
        }
    }

    @Override
    public void notify(SubscriberConsumeOffset consumeOffset) {
        if (consumeOffset.getPushSemaphore().tryAcquire()) {
            executorService.execute(new PublishRunnable(consumeOffset));
        }
    }

    class PublishRunnable implements Runnable {
        private final SubscriberConsumeOffset subscription;
        private final Topic topic;

        public PublishRunnable(SubscriberConsumeOffset subscription) {
            this.subscription = subscription;
            this.topic = subscription.getTopic();
        }

        @Override
        public void run() {
            StoredMessage storedMessage = topic.getMessagesStore().poll(subscription.getNextOffset());
            //消息不存在，或者最新点位已经被消费
            if (storedMessage == null || storedMessage.getOffset() == subscription.getLastOffset()) {
                topic.getConsumerGroup().getIdleSubscribers().offer(subscription);
                subscription.getPushSemaphore().release();
                //有新的消息过来
                if (topic.getMessagesStore().latestOffset() > 0 && topic.getMessagesStore().latestOffset() > subscription.getNextOffset()) {
                    PushListenerImpl.this.notify(topic);
                }
                return;
            }

            //消息处理
            MqttSession session = subscription.getMqttSession();
            System.out.println("分发消息给：" + session);
            MqttPublishMessage publishMessage = MqttMessageBuilders.publish().payload(ByteBuffer.wrap(storedMessage.getPayload())).qos(storedMessage.getMqttQoS()).packetId(session.getPacketIdCreator().getAndIncrement()).topicName(topic.getTopic()).build();
            session.write(publishMessage);

            subscription.setLastOffset(storedMessage.getOffset());
            subscription.setNextOffset(storedMessage.getOffset() + 1);
            //消费下一个
            if (subscription.getNextOffset() <= topic.getMessagesStore().latestOffset()) {
                executorService.execute(this);
            } else {
                run();
            }
        }
    }
}
