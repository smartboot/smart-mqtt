package tech.smartboot.mqtt.auth;

import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.AsyncEventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
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
        log("正在初始化简单认证插件...");
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);
        Map<String, byte[]> accounts = pluginConfig.getAccounts().stream().collect(java.util.stream.Collectors.toMap(PluginConfig.Account::getUsername, account -> account.getPassword().getBytes()));
        enabled = true;
        brokerContext.getEventBus().subscribe(EventType.CONNECT, AsyncEventObject.syncConsumer(new EventBusConsumer<AsyncEventObject<MqttConnectMessage>>() {
            @Override
            public void consumer(EventType<AsyncEventObject<MqttConnectMessage>> eventType, AsyncEventObject<MqttConnectMessage> object) {
                MqttSession session = object.getSession();
                //如果已经认证失败，就不需要再认证了
                if (session.isDisconnect()) {
                    return;
                }
                MqttConnectMessage message = object.getObject();
                String messageUsername = message.getPayload().userName();
                byte[] messagePassword = message.getPayload().passwordInBytes();

                if (messageUsername == null || messagePassword == null) {
                    log("认证失败: 用户名或密码为空");
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                    return;
                }
                if (Arrays.equals(accounts.get(messageUsername), messagePassword)) {
                    session.setAuthorized(true);
                    log("认证成功: " + messageUsername);
                } else {
                    log("认证失败: 用户名或密码错误 - " + messageUsername);
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                }
            }

            @Override
            public boolean enable() {
                return enabled;
            }
        }));
        log("简单认证插件初始化完成，已加载 " + accounts.size() + " 个用户账户");
    }

    @Override
    protected void destroyPlugin() {
        log("正在关闭简单认证插件...");
        enabled = false;
        log("简单认证插件已关闭");
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
