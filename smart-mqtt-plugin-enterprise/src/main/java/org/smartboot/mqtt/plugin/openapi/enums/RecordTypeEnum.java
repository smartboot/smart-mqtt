package org.smartboot.mqtt.plugin.openapi.enums;

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
