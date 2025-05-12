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

public enum RecordTypeEnum {
    DB("db", "数据库"), LOG("log", "日志"), NONE("none", "忽略");
    private String code;
    private String desc;

    RecordTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RecordTypeEnum getByCode(String code) {
        for (RecordTypeEnum statueEnum : values()) {
            if (statueEnum.code.equals(code)) {
                return statueEnum;
            }
        }
        return null;
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
