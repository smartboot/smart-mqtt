package org.smartboot.mqtt.broker.openapi.controller;

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerRuntime;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.enums.BrokerStatueEnum;
import org.smartboot.mqtt.broker.openapi.to.BrokerNodeTO;
import org.smartboot.mqtt.common.enums.MqttMetricEnum;
import org.smartboot.mqtt.common.to.MetricItemTO;
import org.smartboot.mqtt.common.to.MetricTO;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
@Controller
public class DashBoardController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashBoardController.class);
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * 60;
    private static final int DAY = 24 * 60 * 60;
    private final BrokerContext brokerContext;

    public DashBoardController(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @RequestMapping(OpenApi.DASHBOARD_OVERVIEW)
    public RestResult<MetricTO> overview() {
        MetricTO metricTO = new MetricTO();
        Collection<MqttSession> sessions = brokerContext.getSessions();
        metricTO.getMetric().put(MqttMetricEnum.CLIENT_ONLINE.getCode(), brokerContext.metric(MqttMetricEnum.CLIENT_ONLINE));

        metricTO.getMetric().put(MqttMetricEnum.TOPIC_COUNT.getCode(), brokerContext.metric(MqttMetricEnum.TOPIC_COUNT));

        int subCount = 0;
        for (MqttSession session : sessions) {
            subCount += session.getSubscribers().size();
        }
        MetricItemTO subscribeTopicCount = new MetricItemTO();
        subscribeTopicCount.setCode("subscribe_topic_count");
        subscribeTopicCount.setValue(subCount);
        metricTO.getMetric().put(subscribeTopicCount.getCode(), subscribeTopicCount);

        metricTO.getMetric().put(MqttMetricEnum.PERIOD_MESSAGE_RECEIVED.getCode(), brokerContext.metric(MqttMetricEnum.PERIOD_MESSAGE_RECEIVED));
        metricTO.getMetric().put(MqttMetricEnum.PERIOD_MESSAGE_SENT.getCode(), brokerContext.metric(MqttMetricEnum.PERIOD_MESSAGE_SENT));
        return RestResult.ok(metricTO);
    }

    @RequestMapping(OpenApi.DASHBOARD_NODES)
    public RestResult<List<BrokerNodeTO>> nodes() {
        BrokerRuntime brokerRuntime = brokerContext.getRuntime();
        BrokerNodeTO nodeTO = new BrokerNodeTO();
        nodeTO.setName("smart-mqtt@" + (StringUtils.isBlank(brokerContext.getBrokerConfigure().getHost()) ? "::1" : brokerContext.getBrokerConfigure().getHost()));
        nodeTO.setStatus(BrokerStatueEnum.RUNNING.getCode());
        nodeTO.setVersion(BrokerConfigure.VERSION);
        nodeTO.setPid(brokerRuntime.getPid());

        //运行时长
        StringBuilder sb = new StringBuilder();
        long runtime = (System.currentTimeMillis() - brokerRuntime.getStartTime()) / 1000;
        if (runtime >= DAY) {
            sb.append(runtime / DAY).append("天");
            runtime %= DAY;
        }
        if (runtime >= HOUR) {
            sb.append(runtime / HOUR).append("小时");
            runtime %= MINUTE;
        }
        if (runtime >= MINUTE) {
            sb.append(runtime / MINUTE).append("分");
        }
        sb.append(runtime % 60).append("秒");
        nodeTO.setRuntime(sb.toString());


        OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        nodeTO.setCpu((int) (systemMXBean.getSystemCpuLoad() * 100));
        nodeTO.setMemory((int) ((systemMXBean.getTotalPhysicalMemorySize() - systemMXBean.getFreePhysicalMemorySize()) * 100.0 / (systemMXBean.getTotalPhysicalMemorySize())));
        return RestResult.ok(Arrays.asList(nodeTO));
    }

    /**
     * 指标信息
     *
     * @return
     */
    @RequestMapping(OpenApi.DASHBOARD_METRICS)
    public RestResult<MetricTO> metrics() {
        MetricTO metricTO = new MetricTO();
        //连接
        List<MetricItemTO> connectionGroup = new ArrayList<>();
        metricTO.getGroup().put("connection", connectionGroup);
        connectionGroup.add(brokerContext.metric(MqttMetricEnum.CLIENT_CONNECT));
        connectionGroup.add(brokerContext.metric(MqttMetricEnum.CLIENT_DISCONNECT));
        connectionGroup.add(brokerContext.metric(MqttMetricEnum.CLIENT_SUBSCRIBE));
        connectionGroup.add(brokerContext.metric(MqttMetricEnum.CLIENT_UNSUBSCRIBE));

        //会话
        List<MetricItemTO> sessionGroup = new ArrayList<>();
        metricTO.getGroup().put("session", sessionGroup);
        //认证与权限
        List<MetricItemTO> accessGroup = new ArrayList<>();
        metricTO.getGroup().put("access", accessGroup);

        //流量收发
        List<MetricItemTO> bytesGroup = new ArrayList<>();
        metricTO.getGroup().put("bytes", bytesGroup);

        //报文
        List<MetricItemTO> packetGroup = new ArrayList<>();
        metricTO.getGroup().put("packet", packetGroup);
        packetGroup.add(brokerContext.metric(MqttMetricEnum.BYTES_RECEIVED));
        packetGroup.add(brokerContext.metric(MqttMetricEnum.BYTES_SENT));
        packetGroup.add(brokerContext.metric(MqttMetricEnum.PACKETS_RECEIVED));
        packetGroup.add(brokerContext.metric(MqttMetricEnum.PACKETS_SENT));
        packetGroup.add(brokerContext.metric(MqttMetricEnum.PACKETS_CONNECT_RECEIVED));
        packetGroup.add(brokerContext.metric(MqttMetricEnum.PACKETS_CONNACK_SENT));


        //消息数量
        List<MetricItemTO> messageGroup = new ArrayList<>();
        metricTO.getGroup().put("message", messageGroup);
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS0_RECEIVED));
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS1_RECEIVED));
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS2_RECEIVED));
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS0_SENT));
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS1_SENT));
        messageGroup.add(brokerContext.metric(MqttMetricEnum.MESSAGE_QOS2_SENT));

        //消息分发
        List<MetricItemTO> deliveryGroup = new ArrayList<>();
        metricTO.getGroup().put("delivery", deliveryGroup);
        return RestResult.ok(metricTO);
    }

}
