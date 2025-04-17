/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message;

import tech.smartboot.mqtt.common.MqttWriter;
import tech.smartboot.mqtt.common.ToString;
import tech.smartboot.mqtt.common.enums.MqttMessageType;
import tech.smartboot.mqtt.common.enums.MqttQoS;

import java.io.IOException;

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
public class MqttFixedHeader extends ToString {
    public static final MqttFixedHeader CONNECT_HEADER = new MqttFixedHeader(MqttMessageType.CONNECT, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader CONN_ACK_HEADER = new MqttFixedHeader(MqttMessageType.CONNACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.PUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_REC_HEADER = new MqttFixedHeader(MqttMessageType.PUBREC, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PUB_REL_HEADER = new MqttFixedHeader(MqttMessageType.PUBREL, MqttQoS.AT_LEAST_ONCE);
    public static final MqttFixedHeader PUB_REL_HEADER_DUP = new MqttFixedHeader(MqttMessageType.PUBREL, true, MqttQoS.AT_LEAST_ONCE, false);
    public static final MqttFixedHeader PUB_COMP_HEADER = new MqttFixedHeader(MqttMessageType.PUBCOMP, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader SUBSCRIBE_HEADER = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, MqttQoS.AT_LEAST_ONCE);
    public static final MqttFixedHeader SUBSCRIBE_HEADER_DUP = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, true, MqttQoS.AT_LEAST_ONCE, false);
    public static final MqttFixedHeader SUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.SUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader UNSUBSCRIBE_HEADER = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, MqttQoS.AT_LEAST_ONCE);
    public static final MqttFixedHeader UNSUBSCRIBE_HEADER_DUP = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, true, MqttQoS.AT_LEAST_ONCE, false);
    public static final MqttFixedHeader UNSUB_ACK_HEADER = new MqttFixedHeader(MqttMessageType.UNSUBACK, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PING_REQ_HEADER = new MqttFixedHeader(MqttMessageType.PINGREQ, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader PING_RESP_HEADER = new MqttFixedHeader(MqttMessageType.PINGRESP, MqttQoS.AT_MOST_ONCE);
    public static final MqttFixedHeader DISCONNECT_HEADER = new MqttFixedHeader(MqttMessageType.DISCONNECT, MqttQoS.AT_MOST_ONCE);

    public static final MqttFixedHeader PUB_QOS0_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false);
    public static final MqttFixedHeader PUB_RETAIN_QOS0_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, true);
    public static final MqttFixedHeader PUB_QOS1_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false);
    public static final MqttFixedHeader PUB_RETAIN_QOS1_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true);
    public static final MqttFixedHeader PUB_QOS2_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.EXACTLY_ONCE, false);
    public static final MqttFixedHeader PUB_RETAIN_QOS2_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.EXACTLY_ONCE, true);

    public static final MqttFixedHeader PUB_FAILURE_HEADER = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.FAILURE, false);

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

    private final byte encodeByte;

    private MqttFixedHeader(MqttMessageType messageType, boolean dup, MqttQoS qosLevel, boolean retain) {
        this.messageType = messageType;
        this.dup = dup;
        this.qosLevel = qosLevel;
        this.retain = retain;
        this.encodeByte = encodeByte();
    }

    private MqttFixedHeader(MqttMessageType messageType, MqttQoS qosLevel) {
        this(messageType, false, qosLevel, false);
    }

    private byte encodeByte() {
        int ret = 0;
        ret |= messageType.value() << 4;
        if (dup) {
            ret |= 0x08;
        }
        ret |= qosLevel.value() << 1;
        if (retain) {
            ret |= 0x01;
        }
        return ((byte) ret);
    }

    public void writeTo(MqttWriter mqttWriter) throws IOException {
        mqttWriter.writeByte(encodeByte);
    }

    public static MqttFixedHeader getInstance(MqttMessageType messageType, boolean dup, int qosLevel, boolean retain) {
        switch (messageType) {
            case CONNECT:
                return MqttFixedHeader.CONNECT_HEADER;
            case CONNACK:
                return MqttFixedHeader.CONN_ACK_HEADER;
            case SUBSCRIBE:
                return dup ? MqttFixedHeader.SUBSCRIBE_HEADER_DUP : MqttFixedHeader.SUBSCRIBE_HEADER;
            case SUBACK:
                return MqttFixedHeader.SUB_ACK_HEADER;
            case UNSUBACK:
                return MqttFixedHeader.UNSUB_ACK_HEADER;
            case UNSUBSCRIBE:
                return dup ? MqttFixedHeader.UNSUBSCRIBE_HEADER_DUP : MqttFixedHeader.UNSUBSCRIBE_HEADER;
            case PUBLISH:
                if (dup) {
                    return new MqttFixedHeader(messageType, true, MqttQoS.valueOf(qosLevel), retain);
                }
                switch (qosLevel) {
                    case 0:
                        return retain ? MqttFixedHeader.PUB_RETAIN_QOS0_HEADER : MqttFixedHeader.PUB_QOS0_HEADER;
                    case 1:
                        return retain ? MqttFixedHeader.PUB_RETAIN_QOS1_HEADER : MqttFixedHeader.PUB_QOS1_HEADER;
                    case 2:
                        return retain ? MqttFixedHeader.PUB_RETAIN_QOS2_HEADER : MqttFixedHeader.PUB_QOS2_HEADER;
                    default:
                        return MqttFixedHeader.PUB_FAILURE_HEADER;
                }
            case PUBACK:
                return MqttFixedHeader.PUB_ACK_HEADER;
            case PUBREC:
                return MqttFixedHeader.PUB_REC_HEADER;
            case PUBREL:
                return dup ? MqttFixedHeader.PUB_REL_HEADER_DUP : MqttFixedHeader.PUB_REL_HEADER;
            case PUBCOMP:
                return MqttFixedHeader.PUB_COMP_HEADER;
            case PINGREQ:
                return MqttFixedHeader.PING_REQ_HEADER;
            case PINGRESP:
                return MqttFixedHeader.PING_RESP_HEADER;
            case DISCONNECT:
                return MqttFixedHeader.DISCONNECT_HEADER;
            default:
                throw new IllegalArgumentException("unknown message type: " + messageType);
        }
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
}
