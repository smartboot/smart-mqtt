package org.smartboot.mqtt.plugin.openapi.enums;

public enum SaltTypeEnum {
    disable("disable", "不加盐"), PREFIX("prefix", "头部加盐"), SUFFIX("suffix", "尾部加盐");
    private String code;
    private String desc;

    SaltTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SaltTypeEnum getByCode(String code) {
        for (SaltTypeEnum statueEnum : values()) {
            if (statueEnum.code.equals(code)) {
                return statueEnum;
            }
        }
        throw new IllegalArgumentException("");
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
