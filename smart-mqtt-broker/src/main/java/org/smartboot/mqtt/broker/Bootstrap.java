package org.smartboot.mqtt.broker;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        BrokerContext context = new BrokerContextImpl();
        context.init();

        Runtime.getRuntime().addShutdownHook(new Thread(context::destroy));
    }
}
