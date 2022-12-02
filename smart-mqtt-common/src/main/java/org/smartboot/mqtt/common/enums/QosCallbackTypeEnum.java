package org.smartboot.mqtt.common.enums;

/**
 * @author qinluo
 * @date 2022-04-11 16:30:57
 * @since 1.0.0
 */
public enum QosCallbackTypeEnum {

    /**
     * QosLevel 1. (server -> client)
     */
    PUBACK(1, MqttMessageType.PUBACK.value(), 0, null),

    /*
     * PUBLISH QosLevel 2 (client -> server)
     *
     */

    /**
     * QosLevel 2 (server -> client)
     */
    PUBREC(2, MqttMessageType.PUBREC.value(), 0, null),

    /**
     * QosLevel 2 (client -> server)
     */
    PUBREL(3, MqttMessageType.PUBREL.value(), 1, null),

    /**
     * QosLevel 2 (server -> client)
     */
    PUBCOMP(4, MqttMessageType.PUBCOMP.value(), 0, null),

    ;

    static {
        PUBREC.next = PUBREL;
        PUBREL.next = PUBCOMP;
    }

    private final int type;
    private final int msgType;
    private final int waitSide;
    private QosCallbackTypeEnum next;

    QosCallbackTypeEnum(int type, int msgType, int waitSide, QosCallbackTypeEnum next) {
        this.type = type;
        this.msgType = msgType;
        this.waitSide = waitSide;
        this.next = next;
    }

    public int getType() {
        return type;
    }

    public int getMsgType() {
        return msgType;
    }

    public int getWaitSide() {
        return waitSide;
    }

    public static QosCallbackTypeEnum getInstance(int type) {
        for (QosCallbackTypeEnum instance : values()) {
            if (instance.type == type) {
                return instance;
            }
        }

        return null;
    }

    public static QosCallbackTypeEnum next(int type, int side) {
        QosCallbackTypeEnum instance = getInstance(type);
        if (instance == null) {
            return null;
        }

        instance = instance.next;

        while (instance != null) {
            if (instance.waitSide == side) {
                return instance;
            }
            instance = instance.next;
        }

        return null;

    }
}
