package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author 三刀
 * @version v1.0 6/25/25
 */
class ClusterMqttSession implements MqttSession {
    private final String clientId;

    public ClusterMqttSession(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public boolean isDisconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MqttVersion getMqttVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuthorized(boolean authorized) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAuthorized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLatestReceiveMessageTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(MqttMessage mqttMessage, boolean autoFlush) {
        throw new UnsupportedOperationException();
    }
}
