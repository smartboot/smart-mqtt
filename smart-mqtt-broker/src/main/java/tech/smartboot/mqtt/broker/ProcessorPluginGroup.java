package tech.smartboot.mqtt.broker;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.plugin.spec.FlexiblePlugin;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 三刀
 * @version v1.0 3/9/26
 */
class ProcessorPluginGroup implements Plugin<MqttMessage> {
    private final List<FlexiblePlugin> pluginList = new CopyOnWriteArrayList<>();

    public void addPlugin(List<FlexiblePlugin> plugins) {
        pluginList.addAll(plugins);
    }

    public void addPlugin(FlexiblePlugin plugin) {
        pluginList.add(plugin);
    }

    @Override
    public boolean preProcess(AioSession session, MqttMessage mqttMessage) {
        boolean flag = true;
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (!flexiblePlugin.enable()) {
                continue;
            }
            if (!flexiblePlugin.preProcess(session, mqttMessage)) {
                flag = false;
            }
        }
        return flag;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                flexiblePlugin.stateEvent(stateMachineEnum, session, throwable);
            }
        }
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        boolean remove = false;
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                channel = flexiblePlugin.shouldAccept(channel);
                if (channel == null) {
                    return null;
                }
            } else {
                System.out.println(flexiblePlugin + " is disable");
                remove = true;
            }
        }
        if (remove) {
            System.out.println("remove disable plugin");
            pluginList.removeIf(plugin -> !plugin.enable());
        }
        return channel;
    }

    @Override
    public void afterRead(AioSession session, int readSize) {
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                flexiblePlugin.afterRead(session, readSize);
            }
        }
    }

    @Override
    public void beforeRead(AioSession session) {
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                flexiblePlugin.beforeRead(session);
            }
        }
    }

    @Override
    public void afterWrite(AioSession session, int writeSize) {
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                flexiblePlugin.afterWrite(session, writeSize);
            }
        }
    }

    @Override
    public void beforeWrite(AioSession session) {
        for (FlexiblePlugin flexiblePlugin : pluginList) {
            if (flexiblePlugin.enable()) {
                flexiblePlugin.beforeWrite(session);
            }
        }
    }
}
