package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.enums.MqttMessageType;
import org.smartboot.mqtt.common.enums.MqttQoS;

/**
 * 固定报头，每个 MQTT 控制报文都包含一个固定报头。
 * 报头格式:
 * <pre>
 *  _________________________________________
 *  |  Bit  | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
 *  |---------------------------------------|
 *  | byte1 |  报文类型  | 指定报文类型的标志位  |
 *  |---------------------------------------|
 *  | byte2 |           剩余长度              |
 *  |---------------------------------------|
 *  </pre>
 *
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttFixedHeader {
    public static final MqttFixedHeader CONNECT_HEADER = new MqttFixedHeader(MqttMessageType.CONNECT, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader CONN_ACK_HEADER = new MqttFixedHeader(MqttMessageType.CONNACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.PUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_REC_HEADER = new MqttFixedHeader(MqttMessageType.PUBREC, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_REL_HEADER = new MqttFixedHeader(MqttMessageType.PUBREL, MqttQoS.AT_LEAST_ONCE);
    public static final MqttFixedHeader PUB_COMP_HEADER = new MqttFixedHeader(MqttMessageType.PUBCOMP, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader SUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.SUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader UNSUBSCRIBE_HEADER = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, MqttQoS.AT_LEAST_ONCE);
    public static final MqttFixedHeader UNSUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.UNSUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PING_REQ_HEADER = new MqttFixedHeader(MqttMessageType.PINGREQ, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PING_RESP_HEADER = new MqttFixedHeader(MqttMessageType.PINGRESP, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader DISCONNECT_HEADER = new MqttFixedHeader(MqttMessageType.DISCONNECT, MqttQoS.AT_MOST_ONCE);
    private final MqttMessageType messageType;
    /**
     * 重发标志
     */
    private final boolean dup;
    private final MqttQoS qosLevel;
    /**
     * 保留标志，是否存储消息
     */
    private final boolean retain;
    private final int remainingLength;

    public MqttFixedHeader(MqttMessageType messageType, boolean dup, MqttQoS qosLevel, boolean retain, int remainingLength) {
        this.messageType = messageType;
        this.dup = dup;
        this.qosLevel = qosLevel;
        this.retain = retain;
        this.remainingLength = remainingLength;
    }

    public MqttFixedHeader(MqttMessageType messageType, MqttQoS qosLevel) {
        this(messageType, false, qosLevel, false, 0);
    }

    public MqttMessageType getMessageType() {
        return messageType;
    }

    public boolean isDup() {
        return dup;
    }

    public MqttQoS getQosLevel() {
        return qosLevel;
    }

    public boolean isRetain() {
        return retain;
    }

    public int remainingLength() {
        return remainingLength;
    }

}
