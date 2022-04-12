package org.smartboot.mqtt.common;

import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.QosCallbackTypeEnum;

/**
 * @author qinluo
 * @date 2022-04-11 16:25:36
 * @since 1.0.0
 */
public class QosCallback {

    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    /**
     * Message's packetId.
     */
    private final int packetId;

    /**
     * Message's qos level
     * @see org.smartboot.mqtt.common.enums.MqttQoS
     */
    private final int qosLevel;

    /**
     * Qos callback type.
     */
    private int callbackType;

    /**
     * side
     */
    private final int side;

    /**
     * lastest update time in mills.
     */
    private long updateTime;

    private String clientId;

    /**
     * lock.
     */
    private Object lock = new Object();

    public QosCallback(String clientId, int packetId, int qosLevel, int side) {
        this.clientId = clientId;
        this.packetId = packetId;
        this.qosLevel = qosLevel;
        this.side = side;
        this.updateTime = System.currentTimeMillis();

        //  QosLevel 1
        if (qosLevel == MqttQoS.AT_LEAST_ONCE.value() && side == CLIENT) {
            this.callbackType = QosCallbackTypeEnum.PUBACK.getType();
        } else if (side == SERVER && qosLevel == MqttQoS.EXACTLY_ONCE.value()) {
            this.callbackType = QosCallbackTypeEnum.PUBREL.getType();
        } else if (side == CLIENT && qosLevel == MqttQoS.EXACTLY_ONCE.value()) {
            this.callbackType = QosCallbackTypeEnum.PUBREC.getType();
        } else {
            throw new IllegalArgumentException("Unsupported QosLevel " + qosLevel);
        }
    }

    public int getPacketId() {
        return packetId;
    }

    public int getQosLevel() {
        return qosLevel;
    }

    public int getCallbackType() {
        return callbackType;
    }

    public boolean hasNextCallback() {
        return QosCallbackTypeEnum.next(callbackType, side) != null;
    }

    public void nextCallback() {
        QosCallbackTypeEnum next = QosCallbackTypeEnum.next(callbackType, side);
        if (next != null) {
            this.callbackType = next.getType();
        }
    }

    public void update() {
        this.updateTime = System.currentTimeMillis();
    }

    public String getClientId() {
        return clientId;
    }
}
