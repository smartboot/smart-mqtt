package org.smartboot.mqtt.broker;

/**
 * MQTT 运行时信息
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public final class BrokerRuntime {
    private long startTime;

    BrokerRuntime() {
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
