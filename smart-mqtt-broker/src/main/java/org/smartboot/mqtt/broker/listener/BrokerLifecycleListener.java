package org.smartboot.mqtt.broker.listener;

import org.smartboot.mqtt.broker.BrokerContext;

import java.util.EventListener;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/5
 */
public interface BrokerLifecycleListener extends EventListener {
    void onStarted(BrokerContext context);

    void onDestroy(BrokerContext context);
}
