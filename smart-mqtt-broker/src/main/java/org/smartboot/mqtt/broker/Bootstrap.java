/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        //格式化日志时间
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[yyyy-MM-dd HH:mm:ss]");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        BrokerContext context = new BrokerContextImpl();
//        context.getEventBus().subscribe(ServerEventType.BROKER_CONFIGURE_LOADED_EVENT_TYPE, (eventType, configure) -> configure.addPlugin(new MonitorPlugin<>(5)));
        context.init();

        Runtime.getRuntime().addShutdownHook(new Thread(context::destroy));
    }
}
