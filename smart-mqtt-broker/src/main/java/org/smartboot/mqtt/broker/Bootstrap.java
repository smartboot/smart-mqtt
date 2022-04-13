package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.smartboot.mqtt.common.util.ValidateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        Properties brokerProperties = new Properties();
        //加载默认配置
        brokerProperties.load(Bootstrap.class.getClassLoader().getResourceAsStream("smart-mqtt.properties"));
        //加载自定义配置文件
        String brokerConfig = System.getProperty(BrokerConfigure.SystemProperty.BrokerConfig);
        if (StringUtils.isNotBlank(brokerConfig)) {
            File file = new File(brokerConfig);
            ValidateUtils.isTrue(file.isFile(), "文件不存在");
            FileInputStream fileInputStream = new FileInputStream(file);
            brokerProperties.load(fileInputStream);
        }
        BrokerContext context = new BrokerContextImpl(brokerProperties);
        context.init();

        Runtime.getRuntime().addShutdownHook(new Thread(context::destroy));
    }
}
