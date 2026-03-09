package tech.smartboot.mqtt.plugin.spec;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.common.message.MqttMessage;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version v1.0 3/9/26
 */
public abstract class SocketPluginAdapter implements Plugin<MqttMessage> {
    private final Plugin<MqttMessage> plugin;

    public SocketPluginAdapter(Plugin<MqttMessage> plugin) {
        this.plugin = plugin;
    }

    public static SocketPluginAdapter of(Plugin<MqttMessage> plugin) {
        return new SocketPluginAdapter(plugin) {
            @Override
            public boolean enable() {
                return true;
            }
        };
    }

    @Override
    public final boolean preProcess(AioSession session, MqttMessage mqttMessage) {
        return plugin.preProcess(session, mqttMessage);
    }

    @Override
    public final void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
        plugin.stateEvent(stateMachineEnum, session, throwable);
    }

    @Override
    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return plugin.shouldAccept(channel);
    }

    @Override
    public final void afterRead(AioSession session, int readSize) {
        plugin.afterRead(session, readSize);
    }

    @Override
    public final void beforeRead(AioSession session) {
        plugin.beforeRead(session);
    }

    @Override
    public final void afterWrite(AioSession session, int writeSize) {
        plugin.afterWrite(session, writeSize);
    }

    @Override
    public final void beforeWrite(AioSession session) {
        plugin.beforeWrite(session);
    }

    public abstract boolean enable();

    @Override
    public String toString() {
        return "FlexiblePlugin{" +
                "plugin=" + plugin +
                '}';
    }
}
