package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttMessageType {
    Reserved(0, MqttPacketDirectionEnum.Forbid, "保留"),
    CONNECT(1, MqttPacketDirectionEnum.ClientToServer, "客户端请求连接服务端"),
    CONNACK(2, MqttPacketDirectionEnum.ServerToClient, "连接报文确认"),
    PUBLISH(3, MqttPacketDirectionEnum.BothAllowed, "发布消息"),
    PUBACK(4, MqttPacketDirectionEnum.BothAllowed, "QoS1 消息发布收到确认"),
    PUBREC(5, MqttPacketDirectionEnum.BothAllowed, "发布收到（保证交付第一步）"),
    PUBREL(6, MqttPacketDirectionEnum.BothAllowed, "发布释放（保证交付第二步）"),
    PUBCOMP(7, MqttPacketDirectionEnum.BothAllowed, "QoS2 消息发布完成（保证交互第三步）"),
    SUBSCRIBE(8, MqttPacketDirectionEnum.ClientToServer, "客户端订阅请求"),
    SUBACK(9, MqttPacketDirectionEnum.ServerToClient, "订阅请求报文确认"),
    UNSUBSCRIBE(10, MqttPacketDirectionEnum.ClientToServer, "客户端取消订阅请求"),
    UNSUBACK(11, MqttPacketDirectionEnum.ServerToClient, "取消订阅报文确认"),
    PINGREQ(12, MqttPacketDirectionEnum.ClientToServer, "心跳请求"),
    PINGRESP(13, MqttPacketDirectionEnum.ServerToClient, "心跳响应"),
    DISCONNECT(14, MqttPacketDirectionEnum.ClientToServer, "客户端断开连接"),
    Reserved2(15, MqttPacketDirectionEnum.Forbid, "保留");

    private final int value;
    /**
     * 报文流动方向
     */
    private MqttPacketDirectionEnum packetDirection;
    /**
     * 描述
     */
    private String desc;

    public MqttPacketDirectionEnum getPacketDirection() {
        return packetDirection;
    }

    public void setPacketDirection(MqttPacketDirectionEnum packetDirection) {
        this.packetDirection = packetDirection;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    MqttMessageType(int value, MqttPacketDirectionEnum packetDirection, String desc) {
        this.value = value;
        this.packetDirection = packetDirection;
        this.desc = desc;
    }

    public static MqttMessageType valueOf(int type) {
        for (MqttMessageType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown message type: " + type);
    }

    public int value() {
        return value;
    }
}
