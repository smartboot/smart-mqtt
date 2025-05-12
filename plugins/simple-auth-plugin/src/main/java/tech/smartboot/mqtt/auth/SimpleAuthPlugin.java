package tech.smartboot.mqtt.auth;

import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.Arrays;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 4/28/25
 */
public class SimpleAuthPlugin extends Plugin {
    private boolean enabled;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);
        Map<String, byte[]> accounts = pluginConfig.getAccounts().stream().collect(java.util.stream.Collectors.toMap(PluginConfig.Account::getUsername, account -> account.getPassword().getBytes()));
        enabled = true;
        brokerContext.getEventBus().subscribe(EventType.CONNECT, new EventBusConsumer<EventObject<MqttConnectMessage>>() {
            @Override
            public void consumer(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
                MqttSession session = object.getSession();
                //如果已经认证失败，就不需要再认证了
                if (session.isDisconnect()) {
                    return;
                }
                MqttConnectMessage message = object.getObject();
                String messageUsername = message.getPayload().userName();
                byte[] messagePassword = message.getPayload().passwordInBytes();

                System.out.println("<UNK>" + messageUsername + " " + messagePassword);
                if (messageUsername == null || messagePassword == null) {
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                    return;
                }
                if (Arrays.equals(accounts.get(messageUsername), messagePassword)) {
                    session.setAuthorized(true);
                } else {
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                }
            }

            @Override
            public boolean enable() {
                return enabled;
            }
        });
    }

    @Override
    protected void destroyPlugin() {
        enabled = false;
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
        return "simple-auth-plugin";
    }
}
