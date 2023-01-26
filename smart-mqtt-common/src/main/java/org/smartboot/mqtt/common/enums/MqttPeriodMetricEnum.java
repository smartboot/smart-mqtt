package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀（zhengjunweimail@163_com）
 * @version V1_0 , 2023/1/26
 */
public enum MqttPeriodMetricEnum {

    PERIOD_MESSAGE_RECEIVED("period_message_received", "周期内接收消息数"),

    PERIOD_MESSAGE_SENT("period_message_sent", "周期内发送消息数");


    private final String code;
    private final String desc;

    MqttPeriodMetricEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
