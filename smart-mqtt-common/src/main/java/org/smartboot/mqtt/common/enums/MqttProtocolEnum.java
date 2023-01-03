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
        for (MqttProtocolEnum protocolEnum : values()) {
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
