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
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/23
 */
public enum MqttProtocolEnum {
    MQIsdp("MQIsdp"), MQTT("MQTT");
    /**
     * 协议名
     */
    private final String name;

    MqttProtocolEnum(String name) {
        this.name = name;
    }

    public static MqttProtocolEnum getByName(String name) {
        if (MQTT.name.equals(name)) {
            return MQTT;
        }
        if (MQIsdp.name.equals(name)) {
            return MQIsdp;
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
