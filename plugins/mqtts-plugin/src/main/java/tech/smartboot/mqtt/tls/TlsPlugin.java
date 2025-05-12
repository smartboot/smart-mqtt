package tech.smartboot.mqtt.tls;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.common.MqttMessageProcessor;
import tech.smartboot.mqtt.common.MqttProtocol;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.ByteArrayInputStream;

/**
 * @author 三刀
 * @version v1.0 5/7/25
 */
public class TlsPlugin extends Plugin {
    private AioQuickServer server;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);
        Options options = brokerContext.Options();
        MqttMessageProcessor proxy = new MqttMessageProcessor() {
            @Override
            public void process0(AioSession aioSession, MqttMessage mqttMessage) {
                options.getProcessor().process0(aioSession, mqttMessage);
            }

            @Override
            public void stateEvent0(AioSession aioSession, StateMachineEnum stateMachineEnum, Throwable throwable) {
                options.getProcessor().stateEvent0(aioSession, stateMachineEnum, throwable);
            }
        };
        options.getPlugins().forEach(proxy::addPlugin);
        proxy.addPlugin(new SslPlugin<>(new PemServerSSLContextFactory(new ByteArrayInputStream(pluginConfig.getPem().getBytes()))));

        server = new AioQuickServer(options.getHost(), pluginConfig.getPort(), new MqttProtocol(options.getMaxPacketSize()), proxy);
        server.setBannerEnabled(false).setReadBufferSize(options.getBufferSize()).setWriteBuffer(options.getBufferSize(), Math.min(options.getMaxInflight(), 16)).setBufferPagePool(brokerContext.bufferPagePool()).setThreadNum(Math.max(2, options.getThreadNum()));
        if (!options.isLowMemory()) {
            server.disableLowMemory();
        }
        server.start(options.getChannelGroup());

        System.out.println(Options.BANNER + "\r\n ::smart-mqtt broker" + "::\t(" + Options.VERSION + ")");
        if (MqttUtil.isBlank(options.getHost())) {
            System.out.println("\uD83C\uDF89start smart-mqtt tls/ssl success! [port:" + pluginConfig.getPort() + "]");
        } else {
            System.out.println("\uD83C\uDF89start smart-mqtt tls/ssl success! [host:" + options.getHost() + " port:" + pluginConfig.getPort() + "]");
        }
    }

    @Override
    protected void destroyPlugin() {
        server.shutdown();
        System.out.println("mqtts-plugin is shutdown.");
    }

    @Override
    public String getVersion() {
        return Options.VERSION;
    }

    @Override
    public String getVendor() {
        return Options.VENDOR;
    }

    @Override
    public String pluginName() {
        return "mqtts-plugin";
    }
}
