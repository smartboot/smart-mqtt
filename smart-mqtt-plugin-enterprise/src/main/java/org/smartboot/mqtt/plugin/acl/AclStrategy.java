package org.smartboot.mqtt.plugin.acl;

import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.plugin.spec.MqttSession;

public interface AclStrategy {
    void acl(MqttSession session, MqttConnectMessage message);
}
