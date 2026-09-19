/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.controller;

import com.sun.management.OperatingSystemMXBean;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.MqttMessage;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.dao.mapper.ConnectionMapper;
import tech.smartboot.mqtt.plugin.dao.model.MetricDO;
import tech.smartboot.mqtt.plugin.openapi.HistogramMetric;
import tech.smartboot.mqtt.plugin.openapi.enums.MqttMetricEnum;
import tech.smartboot.mqtt.plugin.openapi.to.MetricItemTO;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.BrokerTopic;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.AsyncEventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/21
 */
@Controller
@McpEndpoint(streamableEndpoint = "/mcp/stream", loggingEnable = false)
public class MetricController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricController.class);
    @Autowired
    private BrokerContext brokerContext;

    private final Map<MqttMetricEnum, MetricItemTO> metrics = new HashMap<>();

    private final HistogramMetric publishProcessingDuration = new HistogramMetric(
            "smart_mqtt_publish_processing_duration_seconds",
            "PUBLISH消息处理耗时(秒)",
            new double[]{0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5}
    );

    private final Map<MqttMetricEnum, List<MetricDO>> metricMap = new HashMap<>();
    private static final long START_TIME = System.currentTimeMillis();

    @Autowired
    private PluginConfig pluginConfig;

    @Autowired
    private Plugin plugin;

    @PostConstruct
    public void init() {
        initMetric();
        //周期性重置指标值
        plugin.timer().scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                //推送成功率
                long sent = metrics.get(MqttMetricEnum.PACKETS_PUBLISH_SENT).getValue() - metrics.get(MqttMetricEnum.PACKETS_PUBLISH_SENT).getLatestValue();
                long expect = metrics.get(MqttMetricEnum.PACKETS_EXPECT_PUBLISH_SENT).getValue() - metrics.get(MqttMetricEnum.PACKETS_EXPECT_PUBLISH_SENT).getLatestValue();
                if (expect > 0 && sent < expect) {
                    long rate = sent * 1000 / expect;
//                System.out.println("push success rate:" + (rate) + " , expect:" + expect + " ,sent:" + sent);
                    metrics.get(MqttMetricEnum.PACKETS_PUBLISH_RATE).setValue(rate);
                } else {
//                System.out.println("none push,sent: " + sent);
                    metrics.get(MqttMetricEnum.PACKETS_PUBLISH_RATE).setValue(1000);
                }


                LOGGER.debug("reset period metric...");
                for (Map.Entry<MqttMetricEnum, MetricItemTO> entry : metrics.entrySet()) {
                    MqttMetricEnum metric = entry.getKey();
                    MetricItemTO value = entry.getValue();
                    MetricDO metricDO = new MetricDO();
                    metricDO.setNodeName("smart-mqtt");
                    metricDO.setObjectType("node");
                    metricDO.setObjectId("smart-mqtt");
                    metricDO.setCode(metric.getCode());
                    long currentValue = value.getValue();
                    if (metric.isPeriodRest()) {
                        metricDO.setValue(currentValue - value.getLatestValue());
                    } else {
                        metricDO.setValue(currentValue);
                    }
                    value.setLatestValue(currentValue);
//                LOGGER.info("insert metric:{} value:{}", metricDO.getCode(), metricDO.getValue());
                    List<MetricDO> list = metricMap.computeIfAbsent(entry.getKey(), mqttMetricEnum -> new LinkedList<>());
                    if (list.size() >= 16) {
                        list.remove(0);
                    }
                    metricDO.setCreateTime(new Date(System.currentTimeMillis() / 5000 * 5000));
                    list.add(metricDO);
                }
            }
        }, 5, TimeUnit.SECONDS);

    }

    private void initMetric() {
        for (MqttMetricEnum metricEnum : MqttMetricEnum.values()) {
            metrics.put(metricEnum, new MetricItemTO(metricEnum));
        }

        plugin.addPlugin(new io.github.smartboot.socket.Plugin<MqttMessage>() {
            @Override
            public void afterRead(AioSession session, int readSize) {
                if (readSize > 0) {
                    metrics.get(MqttMetricEnum.BYTES_RECEIVED).getMetric().add(readSize);
                }
            }

            @Override
            public void afterWrite(AioSession session, int writeSize) {
                if (writeSize > 0) {
                    metrics.get(MqttMetricEnum.BYTES_SENT).getMetric().add(writeSize);
                }
            }

            @Override
            public void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
                if (Objects.requireNonNull(stateMachineEnum) == StateMachineEnum.NEW_SESSION) {
                    metrics.get(MqttMetricEnum.CLIENT_ONLINE).getMetric().increment();
                } else if (stateMachineEnum == StateMachineEnum.SESSION_CLOSED) {
                    metrics.get(MqttMetricEnum.CLIENT_ONLINE).getMetric().decrement();
                }
            }
        });
        plugin.subscribe(EventType.CONNECT, AsyncEventObject.syncSubscriber((eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_CONNECT).getMetric().increment()));
        plugin.subscribe(EventType.DISCONNECT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_DISCONNECT).getMetric().increment());
        plugin.subscribe(EventType.SUBSCRIBE_ACCEPT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_SUBSCRIBE).getMetric().increment());
        plugin.subscribe(EventType.UNSUBSCRIBE_ACCEPT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_UNSUBSCRIBE).getMetric().increment());
        plugin.subscribe(EventType.SUBSCRIBE_TOPIC, (eventType, object) -> metrics.get(MqttMetricEnum.SUBSCRIBE_RELATION).getMetric().increment());
        plugin.subscribe(EventType.UNSUBSCRIBE_TOPIC, (eventType, object) -> metrics.get(MqttMetricEnum.SUBSCRIBE_RELATION).getMetric().decrement());
        plugin.subscribe(EventType.RECEIVE_MESSAGE, new EventBusConsumer<EventObject<MqttMessage>>() {
            final LongAdder packetsReceived = metrics.get(MqttMetricEnum.PACKETS_RECEIVED).getMetric();
            final LongAdder connectReceived = metrics.get(MqttMetricEnum.PACKETS_CONNECT_RECEIVED).getMetric();

            @Override
            public void consumer(EventType<EventObject<MqttMessage>> eventType, EventObject<MqttMessage> object) {
                packetsReceived.increment();
                if (object.getObject() instanceof MqttConnectMessage) {
                    connectReceived.increment();
                }
            }
        });
        plugin.subscribe(EventType.WRITE_MESSAGE, new EventBusConsumer<EventObject<MqttMessage>>() {
            final LongAdder packetsSent = metrics.get(MqttMetricEnum.PACKETS_SENT).getMetric();
            final LongAdder connAckSent = metrics.get(MqttMetricEnum.PACKETS_CONNACK_SENT).getMetric();
            final LongAdder publishSent = metrics.get(MqttMetricEnum.PACKETS_PUBLISH_SENT).getMetric();

            final LongAdder qos0Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS0_SENT).getMetric();
            final LongAdder qos1Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS1_SENT).getMetric();
            final LongAdder qos2Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS2_SENT).getMetric();

            @Override
            public void consumer(EventType<EventObject<MqttMessage>> eventType, EventObject<MqttMessage> object) {
                packetsSent.increment();
                if (object.getObject() instanceof MqttConnAckMessage) {
                    connAckSent.increment();
                } else if (object.getObject() instanceof MqttPublishMessage) {
                    publishSent.increment();
                    switch (object.getObject().getFixedHeader().getQosLevel()) {
                        case AT_MOST_ONCE:
                            qos0Sent.increment();
                            break;
                        case AT_LEAST_ONCE:
                            qos1Sent.increment();
                            break;
                        case EXACTLY_ONCE:
                            qos2Sent.increment();
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
            }
        });
        plugin.subscribe(EventType.TOPIC_CREATE, (eventType, object) -> metrics.get(MqttMetricEnum.TOPIC_COUNT).getMetric().increment());
        plugin.subscribe(EventType.PUBLISH_MESSAGE_CONSUME_COST, (eventType, cost) -> publishProcessingDuration.observe(cost / 1_000_000_000D));
        plugin.consumer(new MessageBusConsumer() {
            final LongAdder publishReceived = metrics.get(MqttMetricEnum.PACKETS_PUBLISH_RECEIVED).getMetric();
            final LongAdder expectPublishSent = metrics.get(MqttMetricEnum.PACKETS_EXPECT_PUBLISH_SENT).getMetric();
            final LongAdder qos0Received = metrics.get(MqttMetricEnum.MESSAGE_QOS0_RECEIVED).getMetric();
            final LongAdder qos1Received = metrics.get(MqttMetricEnum.MESSAGE_QOS1_RECEIVED).getMetric();
            final LongAdder qos2Received = metrics.get(MqttMetricEnum.MESSAGE_QOS2_RECEIVED).getMetric();

            @Override
            public void consume(MqttSession session, BrokerTopic topic, Message publishMessage) {
                publishReceived.increment();
                expectPublishSent.add(topic.subscribeCount());
                switch (publishMessage.getQos()) {
                    case AT_MOST_ONCE:
                        qos0Received.increment();
                        break;
                    case AT_LEAST_ONCE:
                        qos1Received.increment();
                        break;
                    case EXACTLY_ONCE:
                        qos2Received.increment();
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        });
    }

    @RequestMapping("/api/cluster/metricCodes")
    public RestResult<List<String>> metrics() throws IOException {
        List<String> showMetrics = pluginConfig.getShowMetrics();
        if (showMetrics == null || showMetrics.isEmpty()) {
            return RestResult.ok(Collections.emptyList());
        } else {
            return RestResult.ok(showMetrics);
        }
    }

    @RequestMapping("/metrics")
    public void prometheus(HttpResponse response) throws IOException {
        StringBuilder builder = new StringBuilder(2048);
        for (MqttMetricEnum metricEnum : MqttMetricEnum.values()) {
            if (!metricEnum.isPrometheusSupport()) {
                continue;
            }
            MetricItemTO metric = metrics.get(metricEnum);
            if (metric == null) {
                continue;
            }
            boolean counter = metricEnum.isPrometheusMetricTypeCounter();
            String name = "smart_mqtt_" + metricEnum.getCode() + (counter ? "_total" : "");
            builder.append("# HELP ").append(name).append(' ').append(metricEnum.getDesc()).append('\n');
            builder.append("# TYPE ").append(name).append(' ').append(counter ? "counter" : "gauge").append('\n');
            builder.append(name).append(' ').append(metric.getValue()).append('\n');
        }

        appendRuntimeMetrics(builder);
        appendNodeInfoMetric(builder);
        publishProcessingDuration.appendPrometheus(builder);

        response.setContentType("text/plain; version=0.0.4; charset=utf-8");
        response.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendRuntimeMetrics(StringBuilder builder) {
        Runtime runtime = Runtime.getRuntime();
        long now = System.currentTimeMillis();

        // 指标命名遵循业界通用做法（Micrometer/Prometheus 客户端库约定）
        appendGauge(builder, "process_uptime_seconds", "进程运行时长(秒)", null, (now - START_TIME) / 1000);
        appendGauge(builder, "process_start_time_seconds", "进程启动时间戳(秒)", null, START_TIME / 1000);

        // JVM 内存指标对齐 JMX 命名（used/committed/max），并通过 area 标签标识内存区域，
        // 便于 Grafana 面板与告警规则复用；未跑满堆时可直观对比 committed 与 max
        appendGauge(builder, "jvm_memory_used_bytes", "JVM已使用内存(字节)", "area=\"heap\"",
                runtime.totalMemory() - runtime.freeMemory());
        appendGauge(builder, "jvm_memory_committed_bytes", "JVM已申请内存(字节)", "area=\"heap\"",
                runtime.totalMemory());
        appendGauge(builder, "jvm_memory_max_bytes", "JVM可申请最大内存(字节)", "area=\"heap\"",
                runtime.maxMemory());

        OperatingSystemMXBean systemMXBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        // CPU 使用率遵循 Prometheus 规范采用 0~1 的 ratio 表示，保留双精度；
        // JMX 采样不可用时返回负值，由 appendCpuGauge 跳过，避免输出无意义样本
        appendCpuGauge(builder, "process_cpu_usage", "进程CPU使用率(0~1)", null, systemMXBean.getProcessCpuLoad());
        appendCpuGauge(builder, "system_cpu_usage", "系统CPU使用率(0~1)", null, systemMXBean.getSystemCpuLoad());
    }

    /**
     * 输出节点元信息指标（info 型指标），版本、JVM、操作系统等信息以标签形式携带。
     * 其余资源指标（process_xxx、jvm_xxx）不加 node/ip/port 标签：
     * Prometheus 采集时已自动附加 instance/job 标签标识采集目标，避免标签冗余。
     */
    private void appendNodeInfoMetric(StringBuilder builder) {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        String labels = "node=\"smart-mqtt\""
                + ",ip=\"" + escapeLabelValue(String.valueOf(brokerContext.Options().getHost())) + "\""
                + ",port=\"" + brokerContext.Options().getPort() + "\""
                + ",version=\"" + escapeLabelValue(Options.VERSION) + "\""
                + ",vm_vendor=\"" + escapeLabelValue(System.getProperty("java.vendor")) + "\""
                + ",vm_version=\"" + escapeLabelValue(System.getProperty("java.version")) + "\""
                + ",os_name=\"" + escapeLabelValue(System.getProperty("os.name")) + "\""
                + ",os_arch=\"" + escapeLabelValue(System.getProperty("os.arch")) + "\""
                + ",hostname=\"" + escapeLabelValue(hostname) + "\"";
        appendGauge(builder, "node_info", "Broker节点信息", labels, 1);
    }

    private void appendGauge(StringBuilder builder, String name, String description, String labels, long value) {
        appendGauge(builder, name, description, labels, (double) value);
    }

    private void appendGauge(StringBuilder builder, String name, String description, String labels, double value) {
        String metricName = "smart_mqtt_" + name;
        builder.append("# HELP ").append(metricName).append(' ').append(description).append('\n');
        builder.append("# TYPE ").append(metricName).append(" gauge\n");
        builder.append(metricName);
        if (labels != null && !labels.isEmpty()) {
            builder.append('{').append(labels).append('}');
        }
        builder.append(' ').append(formatValue(value)).append('\n');
    }

    private void appendCpuGauge(StringBuilder builder, String name, String description, String labels, double load) {
        // JMX 在 CPU 采样不可用时会返回负值，此时跳过该指标
        if (load < 0) {
            return;
        }
        appendGauge(builder, name, description, labels, load);
    }

    private String formatValue(double value) {
        // 整数值以整型输出，避免 Double.toString 产生科学计数法
        if (!Double.isNaN(value) && !Double.isInfinite(value)
                && value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String escapeLabelValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}