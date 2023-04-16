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
public enum MqttTopicFilterEnum {
    /**
     * 精确匹配
     */
    EXACT_MATCH(""),
    /**
     * 多层通配符
     */
    MULTI_LEVEL_MATCH("#"),
    /**
     * 单层通配符
     */
    SINGLE_LEVEL_MATCH("+"),
    /**
     * 应用内部
     */
    INNER_MATCH("$");
    /**
     * 协议名
     */
    private final String name;

    MqttTopicFilterEnum(String name) {
        this.name = name;
    }

    public static MqttTopicFilterEnum getByName(String name) {
        for (MqttTopicFilterEnum protocolEnum : values()) {
            if (protocolEnum.name.equals(name)) {
                return protocolEnum;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
