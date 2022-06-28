package org.smartboot.mqtt.broker.messagebus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AsyncTask;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/4
 */
public class MessageBusImpl implements MessageBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusImpl.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Message[] busQueue = new Message[64];
    private final AtomicLong putOffset = new AtomicLong(-1);
    private boolean running = true;

    @Override
    public void subscribe(Subscriber subscriber) {
        subscribe(subscriber, message -> true);
    }

    @Override
    public void subscribe(Subscriber subscriber, MessageFilter filter) {
        System.out.println("subscribe");
        executorService.execute(new AsyncTask() {
            @Override
            public void execute() {
                System.out.println("execute...");
                //获取最新点位
                long offset = putOffset.get();
                while (running) {
                    Message storedMessage = getNextEventMessage(++offset);
                    if (storedMessage == null) {
                        offset = putOffset.get();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }
                    if (filter.accept(storedMessage)) {
                        subscriber.subscribe(storedMessage);
                    }
                }
            }
        });
    }

    private Message getNextEventMessage(long offset) {
        Message storedMessage = busQueue[(int) (offset % busQueue.length)];
        if (storedMessage == null) {
            return null;
        }
        return storedMessage.getOffset() == offset ? storedMessage : null;
    }

    @Override
    public Message publish(MqttPublishMessage storedMessage) {
        Message stored = new Message(storedMessage, putOffset.incrementAndGet());
        busQueue[(int) (stored.getOffset() % busQueue.length)] = stored;
        return stored;
    }
}
