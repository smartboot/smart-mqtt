package tech.smartboot.mqtt.plugin.spec;

import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

/**
 * @author 三刀
 * @version v1.0 3/3/26
 */
public interface PluginSubscriber {
    <T> void subscribe(EventType<T> type, EventBusConsumer<T> subscriber);
}
