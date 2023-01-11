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

    private final PublishProperties publishProperties;

    public MqttPublishVariableHeader(int packetId, String topicName, PublishProperties publishProperties) {
        super(packetId);
        this.topicName = topicName;
        this.publishProperties = publishProperties;
    }

    public String getTopicName() {
        return topicName;
    }


    public PublishProperties getPublishProperties() {
        return publishProperties;
    }

}
