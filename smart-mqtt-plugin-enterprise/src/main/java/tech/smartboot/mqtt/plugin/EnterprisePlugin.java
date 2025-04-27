/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.dao.DatabasePlugin;
import tech.smartboot.mqtt.plugin.openapi.OpenApiFeature;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/2
 */
public class EnterprisePlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterprisePlugin.class);

    private final List<Feature> features = new ArrayList<>();

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Exception {
        if ("false".equals(System.getenv("ENTERPRISE_ENABLE"))) {
            LOGGER.info("enterprise plugin disabled");
            return;
        }

        Yaml yaml = new Yaml();
        Object object = yaml.load(config());
        PluginConfig config = JSONObject.from(object).to(PluginConfig.class);


        brokerContext.Options().addPlugin(new MonitorPlugin<>(60));
        features.add(new DatabasePlugin(brokerContext));
        // openAPI增强
        features.add(new OpenApiFeature(brokerContext, storage(), config));

        for (Feature feature : features) {
            if (feature.isEnable()) {
                LOGGER.debug("start feature:[{}]", feature.name());
                feature.start();
            }
        }

        brokerContext.getEventBus().subscribe(EventType.BROKER_STARTED, new DisposableEventBusSubscriber<BrokerContext>() {
            @Override
            public void consumer(EventType<BrokerContext> eventType, BrokerContext object) {
                brokerContext.getEventBus().subscribe(EventType.CONNECT, new EventBusConsumer<EventObject<MqttConnectMessage>>() {
                    @Override
                    public void consumer(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
                        MqttSession session = object.getSession();
                        if (!session.isAuthorized() && !session.isDisconnect()) {
                            LOGGER.warn("NOT_AUTHORIZED ,connection fail");
                            MqttSession.connFailAck(MqttConnectReturnCode.NOT_AUTHORIZED, session);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void destroyPlugin() {
        features.forEach(Feature::destroy);
    }


    @Override
    public int order() {
        return super.order() + 1;
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
    public String getDescription() {
        return "smart-mqtt 企业版";
    }


}
