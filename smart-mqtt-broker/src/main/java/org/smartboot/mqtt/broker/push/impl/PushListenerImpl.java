//package org.smartboot.mqtt.broker.push.impl;
//
//import org.smartboot.mqtt.common.MqttMessageBuilders;
//import org.smartboot.mqtt.broker.MqttSession;
//import org.smartboot.mqtt.broker.Topic;
//import org.smartboot.mqtt.common.enums.MqttQoS;
//import org.smartboot.mqtt.common.message.MqttPublishMessage;
//import org.smartboot.mqtt.broker.push.PushListener;
//import org.smartboot.mqtt.broker.push.QosTask;
//import org.smartboot.mqtt.broker.store.StoredMessage;
//import org.smartboot.mqtt.broker.store.SubscriberConsumeOffset;
//
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * @author 三刀（zhengjunweimail@163.com）
// * @version V1.0 , 2022/3/24
// */
//public class PushListenerImpl implements PushListener {
//    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
//    /**
//     * 滑动窗口大小
//     */
//    private long max_inflight = 1;
//
//    @Override
//    public void notify(Topic topic) {
//        ConcurrentLinkedQueue<SubscriberConsumeOffset> queue = topic.getConsumerGroup().getIdleSubscribers();
//        SubscriberConsumeOffset subscriberConsumeOffset;
//        while ((subscriberConsumeOffset = queue.poll()) != null) {
//            if (subscriberConsumeOffset.isEnable()) {
//                notify(subscriberConsumeOffset);
//            }
//        }
//    }
//
//    @Override
//    public void notify(SubscriberConsumeOffset consumeOffset) {
//        if (consumeOffset.getPushSemaphore().tryAcquire()) {
//            executorService.execute(new PublishRunnable(consumeOffset));
//        }
//    }
//
//    class PublishRunnable implements Runnable {
//        private final SubscriberConsumeOffset subscription;
//        private final Topic topic;
//
//        public PublishRunnable(SubscriberConsumeOffset subscription) {
//            this.subscription = subscription;
//            this.topic = subscription.getTopic();
//        }
//
//        @Override
//        public void run() {
//            StoredMessage storedMessage = topic.getMessagesStore().poll(subscription.getNextOffset());
//            //消息不存在，或者最新点位已经被消费
//            if (storedMessage == null || storedMessage.getOffset() == subscription.getLastOffset()) {
//                topic.getConsumerGroup().getIdleSubscribers().offer(subscription);
//                subscription.getPushSemaphore().release();
//                //有新的消息过来
//                if (topic.getMessagesStore().latestOffset() > 0 && topic.getMessagesStore().latestOffset() > subscription.getNextOffset()) {
//                    PushListenerImpl.this.notify(topic);
//                }
//                return;
//            }
//            //消息处理
//            MqttSession session = subscription.getMqttSession();
//
//            int packetId = session.newPacketId();
//            QosTask qosTask = new QosTask(packetId, subscription, PushListenerImpl.this);
//            //注册监听
//            if (storedMessage.getMqttQoS() == MqttQoS.AT_LEAST_ONCE || storedMessage.getMqttQoS() == MqttQoS.EXACTLY_ONCE) {
//                //当待确认ack消息数达到maxInflight时不再继续推送
//                if (subscription.getInFightQueue().size() >= max_inflight) {
//                    subscription.getPushSemaphore().release();
//                    return;
//                }
//                subscription.getInFightQueue().offer(qosTask);
//                session.put(qosTask);
//            }
//
//
//            System.out.println("分发消息给：" + session);
//            MqttPublishMessage publishMessage = MqttMessageBuilders.publish().payload(storedMessage.getPayload()).qos(storedMessage.getMqttQoS()).packetId(packetId).topicName(topic.getTopic()).build();
//            session.write(publishMessage);
//
//            subscription.setLastOffset(storedMessage.getOffset());
//            subscription.setNextOffset(storedMessage.getOffset() + 1);
//
//
//            //消费下一个
//            if (subscription.getNextOffset() <= topic.getMessagesStore().latestOffset()) {
//                executorService.execute(this);
//            } else {
//                run();
//            }
//        }
//    }
//}
