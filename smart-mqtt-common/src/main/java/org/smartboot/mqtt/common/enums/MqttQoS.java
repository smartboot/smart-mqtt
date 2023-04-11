/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttQoS {
    AT_MOST_ONCE(0, "最多分发一次"),
    AT_LEAST_ONCE(1, "至少分发一次"),
    EXACTLY_ONCE(2, "只分发一次"),
    FAILURE(0x80, "暂不支持");

    private final int value;

    private final String desc;

    MqttQoS(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MqttQoS valueOf(int value) {
        for (MqttQoS q : values()) {
            if (q.value == value) {
                return q;
            }
        }
        throw new IllegalArgumentException("invalid QoS: " + value);
    }

    public int value() {
        return value;
    }
}
