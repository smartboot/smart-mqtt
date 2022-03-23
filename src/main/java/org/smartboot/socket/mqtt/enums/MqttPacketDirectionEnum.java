package org.smartboot.socket.mqtt.enums;

/**
 * 报文流动方向
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/23
 */
public enum MqttPacketDirectionEnum {
    Forbid("禁止"),
    ClientToServer("客户端到服务端"),
    ServerToClient("服务端到客户端"),
    BothAllowed("两个方向都允许");
    private final String desc;

    MqttPacketDirectionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
