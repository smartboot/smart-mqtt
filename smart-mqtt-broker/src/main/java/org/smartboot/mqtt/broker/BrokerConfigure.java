package org.smartboot.mqtt.broker;

import java.util.Properties;

/**
 * broker服务配置
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public class BrokerConfigure {
    /**
     * http://patorjk.com/software/taag/
     * Font Name: Puffy
     */
    public static final String BANNER = "\n" +
            "                               _                         _    _       _                  _                  \n" +
            "                              ( )_                      ( )_ ( )_    ( )                ( )                 \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)     ___ ___     _ _ | ,_)| ,_)   | |_    _ __   _   | |/')    __   _ __ \n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     /' _ ` _ `\\ /'_` )| |  | |     | '_`\\ ( '__)/'_`\\ | , <   /'__`\\( '__)\n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    | ( ) ( ) |( (_) || |_ | |_    | |_) )| |  ( (_) )| |\\`\\ (  ___/| |   \n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (_) (_) (_)`\\__, |`\\__)`\\__)   (_,__/'(_)  `\\___/'(_) (_)`\\____)(_)   \n" +
            "                                                     | |                                                    \n" +
            "                                                     (_)                                                    ";
    /**
     * 当前smart-mqtt
     */
    public static final String VERSION = "v1.0.0-SNAPSHOT";

    /**
     * 自定义配置
     */
    private final Properties properties = new Properties();
    /**
     * 端口号
     */
    private int port;
    /**
     * 默认的客户端keep-alive超时时间.
     * 保持连接的实际值是由应用指定的，一般是几分钟。允许的最大值是 18 小时 12 分 15 秒。
     */
    private long maxKeepAliveTime = 60000;
    /**
     * Push线程数
     */
    private int pushThreadNum = Runtime.getRuntime().availableProcessors();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getMaxKeepAliveTime() {
        return maxKeepAliveTime;
    }

    public void setMaxKeepAliveTime(long maxKeepAliveTime) {
        this.maxKeepAliveTime = maxKeepAliveTime;
    }

    public int getPushThreadNum() {
        return pushThreadNum;
    }

    public void setPushThreadNum(int pushThreadNum) {
        this.pushThreadNum = pushThreadNum;
    }

    public Properties getProperties() {
        return properties;
    }
}
