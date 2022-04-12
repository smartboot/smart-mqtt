package org.smartboot.mqtt.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author qinluo
 * @date 2022-04-12 13:38:07
 * @since 1.0.0
 */
public class QosCallbackProcessors {

    private static final Map<Integer, QosCallbackProcessor> CALLBACK_MAP
            = new ConcurrentHashMap<>(8);

    public static QosCallbackProcessor getProcessor(int callbackType) {
        return CALLBACK_MAP.get(callbackType);
    }

    public static void registerProcessor(int callbackType, QosCallbackProcessor processor) {
        CALLBACK_MAP.put(callbackType, processor);
    }

}
