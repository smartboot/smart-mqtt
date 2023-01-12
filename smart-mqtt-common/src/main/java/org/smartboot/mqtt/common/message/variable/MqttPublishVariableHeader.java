package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.variable.properties.PublishProperties;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishVariableHeader extends MqttPacketIdVariableHeader {

    /**
     * PUBLISH 报文中的主题名不能包含通配符
     */
    private final String topicName;

    private PublishProperties properties;

    public MqttPublishVariableHeader(int packetId, String topicName) {
        super(packetId);
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setProperties(PublishProperties properties) {
        this.properties = properties;
    }

    public PublishProperties getProperties() {
        return properties;
    }

}
