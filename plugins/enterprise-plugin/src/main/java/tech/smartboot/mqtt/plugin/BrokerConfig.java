package tech.smartboot.mqtt.plugin;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Options;

/**
 * MQTT Broker 配置类，负责从配置文件加载并初始化 Broker 的各项运行参数。
 */
@Bean
public class BrokerConfig {

    // ==================== 基础连接配置 ====================

    /**
     * MQTT Broker 监听端口，默认值为 1883。
     */
    @Value("${mqtt.port:1883}")
    private int port;

    /**
     * MQTT Broker 监听主机地址，默认绑定所有网卡 (0.0.0.0)。
     */
    @Value("${mqtt.host:0.0.0.0}")
    private String host;

    /**
     * 单个消息包的最大字节数限制。
     */
    @Value("${mqtt.maxPacketSize}")
    private int maxPacketSize;

    /**
     * 客户端最大未确认消息数 (In-flight messages)
     */
    @Value("${mqtt.maxInFlight}")
    private int maxInFlight;

    // ==================== 性能与资源优化配置 ====================

    /**
     * 工作线程池数量。
     */
    @Value("${mqtt.threadNum}")
    private int threadNum;

    /**
     * 网络缓冲区大小。若配置值 <= 0，则使用默认策略。
     */
    @Value("${mqtt.bufferSize}")
    private int bufferSize;

    /**
     * 推送消息的专用线程池数量。
     */
    @Value("${mqtt.pushThreadNum}")
    private int pushThreadNum;

    /**
     * 单客户端允许订阅的主题数量上限。
     */
    @Value("${mqtt.topicLimit}")
    private int topicLimit;

    /**
     * 消息队列最大长度。
     */
    @Value("${mqtt.maxMessageQueueLength}")
    private int maxMessageQueueLength;

    /**
     * 是否开启高性能模式。
     */
    @Value("${mqtt.perfMode}")
    private boolean perfMode;

    @Autowired
    private BrokerContext brokerContext;

    /**
     * 初始化方法：在 Bean 创建后执行，将配置项注入到 Options 对象中。
     */
    @PostConstruct
    public void init() {
        Options options = brokerContext.Options();

        // 设置基础连接参数
        options.setPort(port);
        options.setHost(host);
        if (maxPacketSize > 0) {
            options.setMaxPacketSize(maxPacketSize);
        }

        if (maxInFlight > 0) {
            options.setMaxInflight(maxInFlight);
        }

        // 设置性能优化参数（仅当配置值大于 0 时生效）
        if (threadNum > 0) {
            options.setThreadNum(threadNum);
        }
        if (bufferSize > 0) {
            options.setBufferSize(bufferSize);
        }
        if (pushThreadNum > 0) {
            options.setPushThreadNum(pushThreadNum);
        }
        if (topicLimit > 0) {
            options.setTopicLimit(topicLimit);
        }
        if (maxMessageQueueLength > 0) {
            options.setMaxMessageQueueLength(maxMessageQueueLength);
        }

        // 设置性能模式开关
        options.setPerfMode(perfMode);
    }

    // ==================== Setter 方法（用于测试或动态修改） ====================

    public void setPort(int port) {
        this.port = port;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setPushThreadNum(int pushThreadNum) {
        this.pushThreadNum = pushThreadNum;
    }

    public void setTopicLimit(int topicLimit) {
        this.topicLimit = topicLimit;
    }

    public void setMaxMessageQueueLength(int maxMessageQueueLength) {
        this.maxMessageQueueLength = maxMessageQueueLength;
    }

    public void setPerfMode(boolean perfMode) {
        this.perfMode = perfMode;
    }
}
