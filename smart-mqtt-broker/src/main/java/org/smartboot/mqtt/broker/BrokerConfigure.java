package org.smartboot.mqtt.broker;

import org.smartboot.mqtt.common.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * broker服务配置
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public class BrokerConfigure extends ToString {
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
    public static final String VERSION = "v0.17-SNAPSHOT";

    static final Map<String, String> SystemEnvironments = new HashMap<>();

    {
        SystemEnvironments.put(convertToEnvironment(SystemProperty.PORT), SystemProperty.PORT);
        SystemEnvironments.put(convertToEnvironment(SystemProperty.THREAD_NUM), SystemProperty.THREAD_NUM);
    }

    /**
     * 节点名称
     */
    private String name;
    /**
     * 地址
     */
    private String host;
    /**
     * 端口号
     */
    private int port = 1883;

    /**
     * IO缓冲区大小
     */
    private int bufferSize = 4 * 1024;

    /**
     * topic数量限制
     */
    private int topicLimit = 1024;

    /**
     * MQTT最大报文限制字节数
     */
    private int maxPacketSize = 1048576;

    /**
     * 线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors();
    /**
     * 默认的客户端keep-alive超时时间.
     * 保持连接的实际值是由应用指定的，一般是几分钟。允许的最大值是 18 小时 12 分 15 秒。
     */
    private long maxKeepAliveTime = 600000;
    /**
     * Push线程数
     */
    private int pushThreadNum = Runtime.getRuntime().availableProcessors();

    /**
     * 网络连接建立后，如果服务端在合理的时间内没有收到 CONNECT 报文，服务端应该关闭这个连接。
     * 单位：毫秒
     */
    private int noConnectIdleTimeout = 5000;

    /**
     *
     */
    private int maxInflight = 8;

    /**
     * 客户端连接鉴权账号
     */
    private String username;

    /**
     * 客户端连接鉴权密码
     */
    private String password;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getNoConnectIdleTimeout() {
        return noConnectIdleTimeout;
    }

    public void setNoConnectIdleTimeout(int noConnectIdleTimeout) {
        this.noConnectIdleTimeout = noConnectIdleTimeout;
    }

    public int getMaxInflight() {
        return maxInflight;
    }

    public void setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public int getTopicLimit() {
        return topicLimit;
    }

    public void setTopicLimit(int topicLimit) {
        this.topicLimit = topicLimit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "BrokerConfigure{" +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", maxKeepAliveTime=" + maxKeepAliveTime +
                ", pushThreadNum=" + pushThreadNum +
                ", noConnectIdleTimeout=" + noConnectIdleTimeout +
                ", maxInflight=" + maxInflight +
                '}';
    }

    private String convertToEnvironment(String property) {
        return property.replace(".", "_").toUpperCase();
    }

    public interface SystemProperty {
        /**
         * broker自定义配置文件
         */
        String BrokerConfig = "brokerConfig";
        /**
         * 服务地址
         */
        String HOST = "broker.host";
        /**
         * broker服务端口号
         */
        String PORT = "broker.port";

        /**
         * broker线程数
         */
        String THREAD_NUM = "broker.threadNum";

        /**
         * connect默认超时时间
         */
        String CONNECT_IDLE_TIMEOUT = "broker.connect.idleTimeout";

        /**
         * 最大飞行窗口
         */
        String MAX_INFLIGHT = "broker.maxInflight";

        /**
         * 客户端连接鉴权账号
         */
        String USERNAME = "broker.username";

        /**
         * 客户端连接鉴权密码
         */
        String PASSWORD = "broker.password";
        /**
         * 集群节点数量上限
         */
        String CLUSTER_NODE_LIMIT = "cluster.node.limit";
    }
}
