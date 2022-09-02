package org.smartboot.mqtt.broker;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        //格式化日志时间
        System.setProperty("org.slf4j.simpleLogger.showDateTime","true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat","[yyyy-MM-dd HH:mm:ss]");

        BrokerContext context = new BrokerContextImpl();
        context.init();

        Runtime.getRuntime().addShutdownHook(new Thread(context::destroy));
    }
}
