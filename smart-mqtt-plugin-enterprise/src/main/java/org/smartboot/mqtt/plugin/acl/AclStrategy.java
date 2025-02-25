package org.smartboot.mqtt.plugin.acl;

import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

public interface AclStrategy {
    void acl(MqttSession session, MqttConnectMessage message);
}
