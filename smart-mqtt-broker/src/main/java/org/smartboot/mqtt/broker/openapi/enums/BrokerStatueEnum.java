package org.smartboot.mqtt.broker.openapi.enums;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/4
 */
public enum BrokerStatueEnum {
    RUNNING("running", "运行中"), STOPPED("stopped", "已停止"), UNKNOWN("unknown", "未知");

    private String code;
    private String desc;

    BrokerStatueEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BrokerStatueEnum getByCode(String code) {
        for (BrokerStatueEnum statueEnum : values()) {
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
