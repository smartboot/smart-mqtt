package tech.smartboot.mqtt.plugin.spec.bus;

import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 2/2/26
 */
public class AsyncEventObject<T> extends EventObject<T> {
    private final CompletableFuture<Void> future = new CompletableFuture<>();

    AsyncEventObject(MqttSession session, T object) {
        super(session, object);
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public static <T> EventBusConsumer<AsyncEventObject<T>> syncConsumer(EventBusConsumer<AsyncEventObject<T>> eventBusConsumer) {
        return (eventType, object) -> {
            try {
                eventBusConsumer.consumer(eventType, object);
                object.getFuture().complete(null);
            } catch (Throwable throwable) {
                object.getFuture().completeExceptionally(throwable);
            }
        };
    }
}
