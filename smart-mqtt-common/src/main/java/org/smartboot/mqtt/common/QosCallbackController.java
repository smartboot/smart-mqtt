package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.message.MqttMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author qinluo
 * @date 2022-04-12 13:59:10
 * @since 1.0.0
 */
public class QosCallbackController {

    private final Map<String, QosCallback> qosCallbackMap = new ConcurrentHashMap<>();
    private int maxSize = Integer.MAX_VALUE;

    // TODO 启动一条线程轮训每个QosCallback，触发重试

    public void put(String key, QosCallback callback) {
        if (qosCallbackMap.size() > maxSize) {
            throw new IllegalStateException();
        }

        qosCallbackMap.put(key, callback);
    }

    public QosCallback get(String key) {
        return qosCallbackMap.get(key);
    }

    public void remove(String key) {
        qosCallbackMap.remove(key);
    }

    public void write(MqttMessage message) {

    }

}
