package org.smartboot.socket.mqtt.message;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * 固定报头，每个 MQTT 控制报文都包含一个固定报头。
 *  报头格式:
 *  <pre>
 *  _________________________________________
 *  |  Bit  | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
 *  |---------------------------------------|
 *  | byte1 |  报文类型  | 指定报文类型的标志位  |
 *  |---------------------------------------|
 *  | byte2 |           剩余长度              |
 *  |---------------------------------------|
 *  </pre>
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttFixedHeader {

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

    public MqttFixedHeader(
            MqttMessageType messageType,
            boolean dup,
            MqttQoS qosLevel,
            boolean retain,
            int remainingLength) {
        this.messageType = messageType;
        this.dup = dup;
        this.qosLevel = qosLevel;
        this.retain = retain;
        this.remainingLength = remainingLength;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
