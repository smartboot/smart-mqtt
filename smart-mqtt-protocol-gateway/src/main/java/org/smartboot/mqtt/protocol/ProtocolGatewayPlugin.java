/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.protocol;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.plugin.Plugin;
import org.smartboot.mqtt.common.eventbus.DisposableEventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;

public abstract class ProtocolGatewayPlugin extends Plugin {
    @Override
    protected void initPlugin(BrokerContext brokerContext) {
        brokerContext.getEventBus().subscribe(ServerEventType.BROKER_STARTED, new DisposableEventBusSubscriber<BrokerContext>() {
            @Override
            public void subscribe(EventType<BrokerContext> eventType, BrokerContext object) {
                Config config = init(brokerContext);
                GatewayService gatewayService = new GatewayService(brokerContext);
                startServer(gatewayService);
            }
        });

    }

    public abstract Config init(BrokerContext brokerContext);

    public abstract void startServer(GatewayService server);
}
