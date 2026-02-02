/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.bus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/29
 */
public interface EventBus {

    <T> void subscribe(EventType<T> type, EventBusConsumer<T> subscriber);

    default <T> void subscribe(List<EventType<T>> types, EventBusConsumer<T> subscriber) {
        for (EventType<T> eventType : types) {
            subscribe(eventType, subscriber);
        }
    }

    /**
     * 发布消息至总线
     */
    <T> void publish(EventType<T> eventType, T object);

    /**
     * 发布消息至总线
     */
    default <T> void publish(EventType<T> eventType, T object, List<EventBusConsumer> subscribers) {
        if (object instanceof AsyncEventObject) {
            asyncPublish(eventType, object, subscribers);
        } else {
            syncPublish(eventType, object, subscribers);
        }

    }

    static <T> void syncPublish(EventType<T> eventType, T object, List<EventBusConsumer> subscribers) {
        boolean remove = false;
        for (EventBusConsumer subscriber : subscribers) {
            try {
                if (subscriber.enable()) {
                    subscriber.consumer(eventType, object);
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        if (remove) {
            subscribers.removeIf(eventBusSubscriber -> !eventBusSubscriber.enable());
        }
    }

    static <T> void asyncPublish(EventType<T> eventType, T object, List<EventBusConsumer> subscribers) {
        AsyncEventObject<T> obj = (AsyncEventObject<T>) object;
        boolean remove = false;
        CompletableFuture<Void> prevFuture = null;
        for (EventBusConsumer subscriber : subscribers) {
            try {
                if (subscriber.enable()) {
                    //构造一个新的异步对象
                    AsyncEventObject<T> asy = EventObject.newAsyncEventObject(obj.getSession(), obj.getObject());
                    //任意一个订阅者异常，都将结果反馈给入参对象。
                    asy.getFuture().exceptionally(throwable -> {
                        obj.getFuture().completeExceptionally(throwable);
                        return null;
                    });
                    if (prevFuture == null) {
                        prevFuture = asy.getFuture();
                        subscriber.consumer(eventType, asy);
                    } else {
                        prevFuture = prevFuture.thenAccept(aVoid -> subscriber.consumer(eventType, asy));
                    }
                } else {
                    remove = true;
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        if (prevFuture == null) {
            obj.getFuture().complete(null);
        } else {
            prevFuture.thenAccept(aVoid -> obj.getFuture().complete(null));
        }
        if (remove) {
            subscribers.removeIf(eventBusSubscriber -> !eventBusSubscriber.enable());
        }
    }

}
