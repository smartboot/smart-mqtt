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

import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.extension.plugins.Plugin;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.LinkedList;
import java.util.List;

/**
 * broker服务配置
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public class Options extends ToString {
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
    public static final String VERSION = "v0.41";

    /**
     * 节点ID，集群内唯一
     */
    private String nodeId = "smart-mqtt";
    /**
     * 地址
     */
    private String host = "0.0.0.0";
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
     * MQTT最大报文限制字节数: 1MB
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
    private int noConnectIdleTimeout = 15000;

    /**
     *
     */
    private int maxInflight = 8;

    private int maxMessageQueueLength = 1024;

    private final List<Plugin<MqttMessage>> plugins = new LinkedList<>();

    private AsynchronousChannelGroup channelGroup;


    /**
     * 低内存模式
     */
    private boolean lowMemory = false;

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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Options addPlugin(Plugin<MqttMessage> plugin) {
        plugins.add(plugin);
        return this;
    }

    public List<Plugin<MqttMessage>> getPlugins() {
        return plugins;
    }

    public AsynchronousChannelGroup getChannelGroup() {
        return channelGroup;
    }

    public void setChannelGroup(AsynchronousChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }


    public int getMaxMessageQueueLength() {
        return maxMessageQueueLength;
    }

    public void setMaxMessageQueueLength(int maxMessageQueueLength) {
        this.maxMessageQueueLength = maxMessageQueueLength;
    }


    public boolean isLowMemory() {
        return lowMemory;
    }

    public void setLowMemory(boolean lowMemory) {
        this.lowMemory = lowMemory;
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


    public interface SystemProperty {
        /**
         * broker自定义配置文件
         */
        String BrokerConfig = "brokerConfig";
    }
}
