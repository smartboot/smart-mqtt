package org.smartboot.mqtt.common.eventbus;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/17
 */
public abstract class DisposableEventBusSubscriber<T> implements EventBusSubscriber<T> {
    private boolean enabled = true;

    @Override
    public final boolean enable() {
        try {
            return enabled;
        } finally {
            enabled = false;
        }

    }
}
