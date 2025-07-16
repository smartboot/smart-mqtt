/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec;

import org.smartboot.socket.extension.plugins.Plugin;
import tech.smartboot.mqtt.common.MqttMessageProcessor;
import tech.smartboot.mqtt.common.ToString;
import tech.smartboot.mqtt.common.message.MqttMessage;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.LinkedList;
import java.util.List;

/**
 * MQTT Broker服务配置类，用于管理和配置MQTT服务器的各项参数。
 * <p>
 * 该类提供了MQTT服务器运行所需的所有配置选项，包括但不限于：
 * <ul>
 *   <li>网络连接参数（主机地址、端口号）</li>
 *   <li>性能调优参数（线程池大小、缓冲区配置）</li>
 *   <li>协议限制（最大报文大小、主题数量限制）</li>
 *   <li>会话管理（保活时间、空闲超时）</li>
 *   <li>消息处理（推送线程数、消息队列长度）</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * Options options = new Options();
 * options.setHost("0.0.0.0");
 * options.setPort(1883);
 * options.setMaxKeepAliveTime(600000);
 * </pre>
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
    public static final String VERSION = "v1.3.0";
    public static final String VENDOR = "smart-mqtt";

    /**
     * MQTT服务器监听地址。
     * <p>
     * 可选值：
     * <ul>
     *   <li>"0.0.0.0" - 监听所有网络接口（默认值）</li>
     *   <li>"127.0.0.1" - 仅监听本地回环地址</li>
     *   <li>特定IP地址 - 监听指定网络接口</li>
     * </ul>
     */
    private String host = "0.0.0.0";
    /**
     * MQTT服务器监听端口。
     * <p>
     * 标准端口：
     * <ul>
     *   <li>1883 - 默认的MQTT端口（明文传输）</li>
     *   <li>8883 - MQTT over SSL/TLS端口</li>
     * </ul>
     * 建议在生产环境中使用8883端口并启用SSL/TLS加密。
     */
    private int port = 1883;

    /**
     * 网络IO缓冲区大小（字节）。
     * <p>
     * 该参数影响网络传输性能：
     * <ul>
     *   <li>较大的缓冲区可以提高大消息的传输效率</li>
     *   <li>较小的缓冲区可以减少内存占用</li>
     * </ul>
     * 默认值：4KB (4096字节)
     * <p>
     * 建议根据实际消息大小和并发连接数调整此值。
     */
    private int bufferSize = 4 * 1024;

    /**
     * 单个客户端允许订阅的最大主题数量。
     * <p>
     * 此限制用于防止客户端订阅过多主题导致的资源耗尽。
     * 建议根据以下因素调整：
     * <ul>
     *   <li>服务器内存容量</li>
     *   <li>客户端使用场景</li>
     *   <li>系统总体性能要求</li>
     * </ul>
     * 默认值：1024
     */
    private int topicLimit = 1024;

    /**
     * MQTT报文的最大允许大小（字节）。
     * <p>
     * 符合MQTT v3.1.1协议规范，用于限制单个MQTT报文的大小，包括：
     * <ul>
     *   <li>PUBLISH消息的payload</li>
     *   <li>CONNECT/SUBSCRIBE等控制报文</li>
     * </ul>
     * 默认值：1MB (1048576字节)
     * <p>
     * 注意：过大的报文可能导致：
     * <ul>
     *   <li>内存压力增加</li>
     *   <li>网络传输延迟</li>
     *   <li>客户端处理超时</li>
     * </ul>
     */
    private int maxPacketSize = 1048576;

    /**
     * 服务器工作线程池大小。
     * <p>
     * 用于处理：
     * <ul>
     *   <li>MQTT消息的解码和编码</li>
     *   <li>业务逻辑处理</li>
     *   <li>消息路由转发</li>
     * </ul>
     * 默认值：当前系统可用的处理器核心数
     * <p>
     * 建议：
     * <ul>
     *   <li>IO密集型场景：线程数 = 核心数 * (1 + IO等待时间/CPU时间)</li>
     *   <li>CPU密集型场景：线程数 = 核心数 + 1</li>
     * </ul>
     */
    private int threadNum = Runtime.getRuntime().availableProcessors();
    /**
     * 客户端Keep Alive超时时间（毫秒）。
     * <p>
     * 符合MQTT v3.1.1协议规范3.1.2.10节，用于：
     * <ul>
     *   <li>检测客户端连接是否存活</li>
     *   <li>清理非活动连接</li>
     *   <li>释放服务器资源</li>
     * </ul>
     * 默认值：600000毫秒（10分钟）
     * <p>
     * 重要说明：
     * <ul>
     *   <li>实际超时时间为客户端指定的Keep Alive值的1.5倍</li>
     *   <li>最大允许值：65535秒（约18小时12分15秒）</li>
     *   <li>建议根据网络环境和应用场景适当调整</li>
     * </ul>
     */
    private long maxKeepAliveTime = 600000;
    /**
     * 消息推送专用线程池大小。
     * <p>
     * 用于处理MQTT消息的推送任务：
     * <ul>
     *   <li>QoS 0消息的直接推送</li>
     *   <li>QoS 1/2消息的重传和确认</li>
     *   <li>保留消息的推送</li>
     * </ul>
     * 默认值：当前系统可用的处理器核心数
     * <p>
     * 调优建议：
     * <ul>
     *   <li>高并发场景：适当增加线程数</li>
     *   <li>注意监控线程池队列长度</li>
     *   <li>考虑消息推送延迟要求</li>
     * </ul>
     */
    private int pushThreadNum = Runtime.getRuntime().availableProcessors();

    /**
     * 等待客户端CONNECT报文的最大时间（毫秒）。
     * <p>
     * 符合MQTT v3.1.1协议规范3.1.4节，用于：
     * <ul>
     *   <li>防止TCP连接建立后不发送CONNECT的僵尸连接</li>
     *   <li>快速释放无效的网络连接</li>
     *   <li>提高服务器资源利用率</li>
     * </ul>
     * 默认值：15000毫秒（15秒）
     * <p>
     * 注意事项：
     * <ul>
     *   <li>值过小可能导致正常客户端连接失败</li>
     *   <li>值过大会延长无效连接的释放时间</li>
     *   <li>建议根据网络质量调整</li>
     * </ul>
     */
    private int noConnectIdleTimeout = 15000;

    /**
     * 最大允许的飞行窗口大小。
     * <p>
     * 用于控制QoS 1和QoS 2消息的传输：
     * <ul>
     *   <li>限制同时在传输过程中的消息数量</li>
     *   <li>实现流量控制</li>
     *   <li>防止消息堆积和内存溢出</li>
     * </ul>
     * 默认值：8
     * <p>
     * 调优建议：
     * <ul>
     *   <li>高吞吐量场景可适当增大</li>
     *   <li>考虑网络延迟和客户端处理能力</li>
     *   <li>过大的值可能导致消息重传增加</li>
     * </ul>
     */
    private int maxInflight = 8;

    /**
     * 单个客户端的最大消息队列长度。
     * <p>
     * 用于限制每个客户端的消息缓存数量：
     * <ul>
     *   <li>离线消息的存储</li>
     *   <li>QoS 1/2消息的重传队列</li>
     *   <li>消息推送缓冲区</li>
     * </ul>
     * 默认值：1024
     * <p>
     * 超出限制时的处理策略：
     * <ul>
     *   <li>丢弃新消息</li>
     *   <li>断开客户端连接</li>
     *   <li>触发流控机制</li>
     * </ul>
     */
    private int maxMessageQueueLength = 1024;

    /**
     * MQTT消息处理插件列表。
     * <p>
     * 支持通过插件机制扩展功能：
     * <ul>
     *   <li>消息持久化</li>
     *   <li>认证授权</li>
     *   <li>消息转换</li>
     *   <li>监控统计</li>
     * </ul>
     */
    private final List<Plugin<MqttMessage>> plugins = new LinkedList<>();

    /**
     * 异步通道组，用于管理网络IO操作。
     * <p>
     * 提供以下功能：
     * <ul>
     *   <li>异步IO操作的线程池</li>
     *   <li>网络连接的管理</li>
     *   <li>IO事件的分发</li>
     * </ul>
     */
    private AsynchronousChannelGroup channelGroup;


    /**
     * 低内存运行模式开关。
     * <p>
     * 启用后的优化策略：
     * <ul>
     *   <li>减少消息缓存</li>
     *   <li>更激进的内存回收</li>
     *   <li>限制并发连接数</li>
     *   <li>禁用部分非必要功能</li>
     * </ul>
     * 默认值：false
     * <p>
     * 建议在以下场景启用：
     * <ul>
     *   <li>资源受限的设备</li>
     *   <li>容器化部署</li>
     *   <li>高密度部署</li>
     * </ul>
     */
    private boolean lowMemory = false;

    private MqttMessageProcessor processor;

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

    public MqttMessageProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(MqttMessageProcessor processor) {
        this.processor = processor;
    }

    public interface SystemProperty {
        /**
         * broker自定义配置文件
         */
        String BrokerConfig = "brokerConfig";
    }
}
