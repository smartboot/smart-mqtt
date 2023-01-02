package org.smartboot.mqtt.common.message;

public class MqttPubReplyVariableHeader extends MqttPacketIdVariableHeader{

    private final byte reasonCode;
    private final MqttProperties properties;

    public MqttPubReplyVariableHeader(int packetId, byte reasonCode, MqttProperties properties) {
        super(packetId);
        if (packetId < 1 || packetId > 0xffff) {
            throw new IllegalArgumentException("packetId: " + packetId + " (should be: 1 ~ 65535)");
        }
        this.reasonCode = reasonCode;
        this.properties = properties;
    }

    public byte getReasonCode() {
        return reasonCode;
    }

    public MqttProperties getProperties() {
        return properties;
    }
}
