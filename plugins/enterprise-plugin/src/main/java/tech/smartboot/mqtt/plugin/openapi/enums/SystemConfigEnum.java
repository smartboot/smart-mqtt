/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.enums;

public enum SystemConfigEnum {
    ACL("acl", "连接认证"),
    LICENSE("license", "授权LICENSE"),
    CONNECT_RECORD("connectRecord", "连接记录"),
    SUBSCRIBE_RECORD("subscribeRecord", "订阅记录"),

    METRIC_RECORD("metricRecord", "指标记录"),

    WS_MQTT_CONFIG("wsMqttConfig", "Websocket配置"),

    SHOW_METRICS("showMetrics", "显示的指标项"),
    UNKNOWN("unknown", "未知");

    private String code;
    private String desc;

    SystemConfigEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SystemConfigEnum getByCode(String code) {
        for (SystemConfigEnum statueEnum : values()) {
            if (statueEnum.code.equals(code)) {
                return statueEnum;
            }
        }
        return UNKNOWN;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
