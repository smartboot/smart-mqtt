package org.smartboot.mqtt.plugin.acl;

import org.smartboot.mqtt.common.MqttSession;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

public interface AclStrategy {
    void acl(MqttSession session, MqttConnectMessage message);
}
