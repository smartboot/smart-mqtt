package org.smartboot.mqtt.plugin.openapi.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.plugin.convert.NodeConvert;
import org.smartboot.mqtt.plugin.dao.MybatisSessionFactory;
import org.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import org.smartboot.mqtt.plugin.dao.mapper.ConnectionMapper;
import org.smartboot.mqtt.plugin.dao.mapper.MetricMapper;
import org.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import org.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import org.smartboot.mqtt.plugin.dao.model.MetricDO;
import org.smartboot.mqtt.plugin.dao.model.RegionDO;
import org.smartboot.mqtt.plugin.openapi.OpenApi;
import org.smartboot.mqtt.plugin.openapi.enums.BrokerStatueEnum;
import org.smartboot.mqtt.plugin.openapi.enums.MqttMetricEnum;
import org.smartboot.mqtt.plugin.openapi.enums.RecordTypeEnum;
import org.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import org.smartboot.mqtt.plugin.openapi.to.BrokerNodeTO;
import org.smartboot.mqtt.plugin.openapi.to.MetricItemTO;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.MqttSession;
import org.smartboot.mqtt.plugin.spec.bus.Consumer;
import org.smartboot.mqtt.plugin.spec.bus.EventBus;
import org.smartboot.mqtt.plugin.spec.bus.EventBusSubscriber;
import org.smartboot.mqtt.plugin.spec.bus.EventObject;
import org.smartboot.mqtt.plugin.spec.bus.EventType;
import org.smartboot.mqtt.plugin.spec.bus.Message;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.AbstractPlugin;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/21
 */
@Controller(async = true)
public class MetricController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricController.class);
    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private MetricMapper metricMapper;

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private BrokerNodeMapper brokerNodeMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    private final Map<MqttMetricEnum, MetricItemTO> metrics = new HashMap<>();
    private boolean h2;

    private RecordTypeEnum recordTypeEnum;

    private final Map<MqttMetricEnum, List<MetricDO>> metricMap = new HashMap<>();
    private static final long START_TIME = System.currentTimeMillis();

    @PostConstruct
    public void init() {
        h2 = System.getProperty(MybatisSessionFactory.CONFIG_JDBC_DB_TYPE, "").contains("h2");
        initMetric(brokerContext);
        recordTypeEnum = RecordTypeEnum.getByCode(systemConfigMapper.getConfig(SystemConfigEnum.METRIC_RECORD.getCode()));
        //周期性重置指标值
        brokerContext.getTimer().scheduleWithFixedDelay(() -> {
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
                metricDO.setNodeName(brokerContext.Options().getNodeId());
                metricDO.setObjectType("node");
                metricDO.setObjectId(brokerContext.Options().getNodeId());
                metricDO.setCode(metric.getCode());
                long currentValue = value.getValue();
                if (metric.isPeriodRest()) {
                    metricDO.setValue(currentValue - value.getLatestValue());
                } else {
                    metricDO.setValue(currentValue);
                }
                value.setLatestValue(currentValue);
//                LOGGER.info("insert metric:{} value:{}", metricDO.getCode(), metricDO.getValue());
                if (recordTypeEnum == RecordTypeEnum.DB) {
                    metricMapper.insert(metricDO);
                } else {
                    List<MetricDO> list = metricMap.computeIfAbsent(entry.getKey(), mqttMetricEnum -> new LinkedList<>());
                    if (list.size() >= 16) {
                        list.remove(0);
                    }
                    metricDO.setCreateTime(new Date(System.currentTimeMillis() / 5000 * 5000));
                    list.add(metricDO);
                }
            }
            if (recordTypeEnum == RecordTypeEnum.DB) {
                //定期清除3天前的数据
                int count = metricMapper.deleteBefore(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)));
                LOGGER.debug("clean {} metric data", count);
                count = metricMapper.clearBefore(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)));
                LOGGER.debug("clean {} metric data", count);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void initMetric(BrokerContext context) {
        for (MqttMetricEnum metricEnum : MqttMetricEnum.values()) {
            metrics.put(metricEnum, new MetricItemTO(metricEnum));
        }

        context.Options().addPlugin(new AbstractPlugin<MqttMessage>() {
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
        EventBus eventBus = context.getEventBus();
        eventBus.subscribe(EventType.CONNECT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_CONNECT).getMetric().increment());
        eventBus.subscribe(EventType.DISCONNECT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_DISCONNECT).getMetric().increment());
        eventBus.subscribe(EventType.SUBSCRIBE_ACCEPT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_SUBSCRIBE).getMetric().increment());
        eventBus.subscribe(EventType.UNSUBSCRIBE_ACCEPT, (eventType, object) -> metrics.get(MqttMetricEnum.CLIENT_UNSUBSCRIBE).getMetric().increment());
        eventBus.subscribe(EventType.SUBSCRIBE_TOPIC, (eventType, object) -> metrics.get(MqttMetricEnum.SUBSCRIBE_RELATION).getMetric().increment());
        eventBus.subscribe(EventType.UNSUBSCRIBE_TOPIC, (eventType, object) -> metrics.get(MqttMetricEnum.SUBSCRIBE_RELATION).getMetric().decrement());
        eventBus.subscribe(EventType.RECEIVE_MESSAGE, new EventBusSubscriber<EventObject<MqttMessage>>() {
            final LongAdder packetsReceived = metrics.get(MqttMetricEnum.PACKETS_RECEIVED).getMetric();
            final LongAdder connectReceived = metrics.get(MqttMetricEnum.PACKETS_CONNECT_RECEIVED).getMetric();

            @Override
            public void subscribe(EventType<EventObject<MqttMessage>> eventType, EventObject<MqttMessage> object) {
                packetsReceived.increment();
                if (object.getObject() instanceof MqttConnectMessage) {
                    connectReceived.increment();
                }
            }
        });
        eventBus.subscribe(EventType.WRITE_MESSAGE, new EventBusSubscriber<EventObject<MqttMessage>>() {
            final LongAdder packetsSent = metrics.get(MqttMetricEnum.PACKETS_SENT).getMetric();
            final LongAdder connAckSent = metrics.get(MqttMetricEnum.PACKETS_CONNACK_SENT).getMetric();
            final LongAdder publishSent = metrics.get(MqttMetricEnum.PACKETS_PUBLISH_SENT).getMetric();

            final LongAdder qos0Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS0_SENT).getMetric();
            final LongAdder qos1Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS1_SENT).getMetric();
            final LongAdder qos2Sent = metrics.get(MqttMetricEnum.MESSAGE_QOS2_SENT).getMetric();

            @Override
            public void subscribe(EventType<EventObject<MqttMessage>> eventType, EventObject<MqttMessage> object) {
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
        eventBus.subscribe(EventType.TOPIC_CREATE, (eventType, object) -> metrics.get(MqttMetricEnum.TOPIC_COUNT).getMetric().increment());
        context.getMessageBus().consumer(new Consumer() {
            final LongAdder publishReceived = metrics.get(MqttMetricEnum.PACKETS_PUBLISH_RECEIVED).getMetric();
            final LongAdder expectPublishSent = metrics.get(MqttMetricEnum.PACKETS_EXPECT_PUBLISH_SENT).getMetric();
            final LongAdder qos0Received = metrics.get(MqttMetricEnum.MESSAGE_QOS0_RECEIVED).getMetric();
            final LongAdder qos1Received = metrics.get(MqttMetricEnum.MESSAGE_QOS1_RECEIVED).getMetric();
            final LongAdder qos2Received = metrics.get(MqttMetricEnum.MESSAGE_QOS2_RECEIVED).getMetric();

            @Override
            public void consume(MqttSession session, Message publishMessage) {
                publishReceived.increment();
                expectPublishSent.add(publishMessage.getTopic().subscribeCount());
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

    @RequestMapping("/api/metric/clear")
    public RestResult<Void> clear() {

        return RestResult.ok(null);
    }


    @RequestMapping("/api/cluster/nodes")
    public RestResult<Collection<BrokerNodeTO>> nodes() {
        //broker节点
        List<BrokerNodeDO> nodes = brokerNodeMapper.selectAll();
        if (CollectionUtils.isEmpty(nodes)) {
            nodes = Collections.emptyList();
        }
        List<BrokerNodeTO> list = NodeConvert.convert(nodes);
        list.stream().filter(brokerNodeTO -> BrokerStatueEnum.STOPPED.getCode().equals(brokerNodeTO.getStatus()) || BrokerStatueEnum.UNKNOWN.getCode().equals(brokerNodeTO.getStatus())).forEach(brokerNodeTO -> {
            brokerNodeTO.setRuntime("-");
            brokerNodeTO.setPid("-");
            brokerNodeTO.setCpuUsage(0);
            brokerNodeTO.setMemUsage(0);
        });
        return RestResult.ok(list);
    }

    @RequestMapping("/api/cluster/metricCodes")
    public RestResult<List<String>> metrics() throws IOException {
        String showMetrics = systemConfigMapper.getConfig(SystemConfigEnum.SHOW_METRICS.getCode());
        if (StringUtils.isBlank(showMetrics)) {
            return RestResult.ok(Collections.emptyList());
        } else {
            return RestResult.ok(Arrays.asList(StringUtils.split(showMetrics, ",")));
        }
    }

    @RequestMapping("/api/cluster/metrics")
    public RestResult<JSONObject> clusterMetrics(@Param("nodeId") String nodeId, @Param("metrics") List<String> metrics, @Param("startTime") Date startTime, @Param("endTime") Date endTime) throws IOException {
        if (CollectionUtils.isEmpty(metrics)) {
            return RestResult.ok(new JSONObject());
        }
        long step = ((endTime.getTime() - startTime.getTime()) / 1000 / 60);
        if (step < 1) {
            step = 1;
        }
        List<MetricDO> publishReceiveCount;
        if (RecordTypeEnum.DB != recordTypeEnum) {
            publishReceiveCount = new ArrayList<>();
            if ((System.currentTimeMillis() - START_TIME) > 10000) {
                metrics.forEach(code -> publishReceiveCount.addAll(metricMap.get(MqttMetricEnum.getByCode(code))));
            }

        } else if (h2) {
            publishReceiveCount = metricMapper.selectH2Metrics(nodeId, metrics, step, startTime, endTime);
        } else {
            publishReceiveCount = metricMapper.selectMetrics(nodeId, metrics, step, startTime, endTime);
        }
        JSONObject jsonObject = new JSONObject();
        publishReceiveCount.stream().collect(Collectors.groupingBy(MetricDO::getCode)).forEach((metric, list) -> {
            Map<Date, Map> group = new HashMap<>();
            list.stream().collect(Collectors.groupingBy(MetricDO::getCreateTime)).forEach((date, sublist) -> {
                Map<String, Object> nodesMetric = new HashMap<>();
                nodesMetric.put("date", date);
                sublist.forEach(metricDO -> nodesMetric.put(metricDO.getNodeName(), metricDO.getValue()));
                group.put(date, nodesMetric);
            });
            List l = new ArrayList();
            group.keySet().stream().sorted().forEach(date -> l.add(group.get(date)));
            jsonObject.put(metric, l);
        });

        return RestResult.ok(jsonObject);
    }

    @RequestMapping("/api/metric/region")
    public RestResult<List<RegionDO>> region() {
        LOGGER.info("Connection Summary:{}", JSON.toJSONString(connectionMapper.groupByProvince()));
        List<RegionDO> list = connectionMapper.groupByProvince();
        list.forEach(regionDO -> regionDO.setName(regionDO.getName().replace("省", "")));
        return RestResult.ok(list);
    }


    @RequestMapping(OpenApi.BASE_API + "/metric/exported")
    public Object exported(@Param("style") String style) {
        if (style == null || style.trim().isEmpty()) {
            style = "PROMETHEUS";
        }

        if (!Objects.equals(style, "PROMETHEUS")) {
            return RestResult.fail("unsupported style " + style);
        }

        StringBuilder sb = new StringBuilder();
        for (MqttMetricEnum metricEnum : MqttMetricEnum.values()) {
            MetricItemTO metric = metrics.get(metricEnum);

            // Single metric example.
            //# HELP mqtt2_mqtt_hello_bytes_total test
            //# TYPE mqtt2_mqtt_hello_bytes_total counter
            //mqtt2_mqtt_hello_bytes_total{node="node",ip="ip",} 0.0
            //# HELP mqtt2_mqtt_hello_bytes_created test
            //# TYPE mqtt2_mqtt_hello_bytes_created gauge
            //mqtt2_mqtt_hello_bytes_created{node="node",ip="ip",} 1.685426524877E9

            sb.append("# HELP ").append(metricEnum.getCode()).append("_total ").append(metric.getDesc()).append("\n").append("# TYPE ").append(metricEnum.getCode()).append("_total ").append(
                    "counter\n").append(metricEnum.getCode()).append("_total ").append("{").append("node=\"").append(brokerContext.Options().getNodeId()).append("\"").append(",ip=\"").append(brokerContext.Options().getHost()).append("\",} ").append(metric.getValue()).append("\n").append("# HELP ").append(metricEnum.getCode()).append("_created ").append(metric.getDesc()).append("\n").append("# TYPE ").append(metricEnum.getCode()).append("_created ").append("gauge\n").append(metricEnum.getCode()).append("_created ").append("{").append("node=\"").append(brokerContext.Options().getNodeId()).append("\"").append(",ip=\"").append(brokerContext.Options().getHost()).append("\",} ").append(metric.getCreated().getTime()).append("\n");
        }


        return sb.toString();
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setMetricMapper(MetricMapper metricMapper) {
        this.metricMapper = metricMapper;
    }

    public void setConnectionMapper(ConnectionMapper connectionMapper) {
        this.connectionMapper = connectionMapper;
    }

    public void setBrokerNodeMapper(BrokerNodeMapper brokerNodeMapper) {
        this.brokerNodeMapper = brokerNodeMapper;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
