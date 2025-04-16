/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.plugin.dao.DatabasePlugin;
import org.smartboot.mqtt.plugin.openapi.OpenApiFeature;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.MqttSession;
import org.smartboot.mqtt.plugin.spec.Plugin;
import org.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import org.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import org.smartboot.mqtt.plugin.spec.bus.EventObject;
import org.smartboot.mqtt.plugin.spec.bus.EventType;
import org.smartboot.socket.extension.plugins.MonitorPlugin;

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
        brokerContext.Options().addPlugin(new MonitorPlugin<>(60));
        features.add(new DatabasePlugin(brokerContext));
        // openAPI增强
        features.add(new OpenApiFeature(brokerContext));

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


}
