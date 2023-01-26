package org.smartboot.mqtt.broker.openapi.controller;

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang.StringUtils;
import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerRuntime;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.eventbus.ServerEventType;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.BrokerNodeTO;
import org.smartboot.mqtt.broker.openapi.to.MetricItemTO;
import org.smartboot.mqtt.broker.openapi.to.MetricTO;
import org.smartboot.mqtt.broker.openapi.to.OverViewTO;
import org.smartboot.mqtt.common.enums.MqttMetricEnum;
import org.smartboot.mqtt.common.eventbus.EventBus;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnAckMessage;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.extension.plugins.AbstractPlugin;
import org.smartboot.socket.transport.AioSession;

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
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * 60;
    private static final int DAY = 24 * 60 * 60;
    private final BrokerContext brokerContext;
    /**
     * 客户端连接次数
     */
    private final MetricItemTO connectMetric = new MetricItemTO(MqttMetricEnum.CLIENT_CONNECT);
    /**
     * 客户端断开连接次数
     */
    private final MetricItemTO disconnectMetric = new MetricItemTO(MqttMetricEnum.CLIENT_DISCONNECT);

    /**
     * 订阅次数
     */
    private final MetricItemTO subscribeMetric = new MetricItemTO(MqttMetricEnum.CLIENT_SUBSCRIBE);

    /**
     * 取消订阅次数
     */
    private final MetricItemTO unsubscribeMetric = new MetricItemTO(MqttMetricEnum.CLIENT_UNSUBSCRIBE);

    /**
     * 流入字节数
     */
    private final MetricItemTO bytesReceivedMetric = new MetricItemTO(MqttMetricEnum.BYTES_RECEIVED);
    /**
     * 流出字节数
     */
    private final MetricItemTO bytesSentMetric = new MetricItemTO(MqttMetricEnum.BYTES_SENT);

    /**
     * 接收的 CONNECT 报文数量
     */
    private final MetricItemTO connReceiveMetric = new MetricItemTO(MqttMetricEnum.PACKETS_CONNECT_RECEIVED);
    /**
     * 发送的 CONNACK 报文数量
     */
    private final MetricItemTO connAckSentMetric = new MetricItemTO(MqttMetricEnum.PACKETS_CONNACK_SENT);
    private final MetricTO metricTO = new MetricTO();

    public DashBoardController(BrokerContext brokerContext) {
        brokerContext.getMessageProcessor().addPlugin(new AbstractPlugin<MqttMessage>() {
            @Override
            public void afterRead(AioSession session, int readSize) {
                if (readSize > 0) {
                    bytesReceivedMetric.getMetric().add(readSize);
                }
            }

            @Override
            public void afterWrite(AioSession session, int writeSize) {
                if (writeSize > 0) {
                    bytesSentMetric.getMetric().add(writeSize);
                }
            }
        });
        this.brokerContext = brokerContext;
        EventBus eventBus = brokerContext.getEventBus();
        eventBus.subscribe(ServerEventType.CONNECT, (eventType, object) -> connectMetric.getMetric().increment());
        eventBus.subscribe(ServerEventType.DISCONNECT, (eventType, object) -> disconnectMetric.getMetric().increment());
        eventBus.subscribe(ServerEventType.SUBSCRIBE_ACCEPT, (eventType, object) -> subscribeMetric.getMetric().increment());
        eventBus.subscribe(ServerEventType.UNSUBSCRIBE_ACCEPT, (eventType, object) -> unsubscribeMetric.getMetric().increment());
        eventBus.subscribe(EventType.RECEIVE_MESSAGE, (eventType, object) -> {
            if (object.getObject() instanceof MqttConnectMessage) {
                connReceiveMetric.getMetric().increment();
            }
        });
        eventBus.subscribe(EventType.WRITE_MESSAGE, (eventType, object) -> {
            if (object.getObject() instanceof MqttConnAckMessage) {
                connAckSentMetric.getMetric().increment();
            }
        });


        //连接
        List<MetricItemTO> connectionGroup = new ArrayList<>();
        metricTO.getGroup().put("connection", connectionGroup);
        connectionGroup.add(connectMetric);
        connectionGroup.add(disconnectMetric);
        connectionGroup.add(subscribeMetric);
        connectionGroup.add(unsubscribeMetric);

        //会话
        List<MetricItemTO> sessionGroup = new ArrayList<>();
        metricTO.getGroup().put("session", sessionGroup);
        //认证与权限
        List<MetricItemTO> accessGroup = new ArrayList<>();
        metricTO.getGroup().put("access", accessGroup);

        //流量收发
        List<MetricItemTO> bytesGroup = new ArrayList<>();
        metricTO.getGroup().put("bytes", bytesGroup);
        bytesGroup.add(bytesReceivedMetric);
        bytesGroup.add(bytesSentMetric);
        //报文
        List<MetricItemTO> packetGroup = new ArrayList<>();
        metricTO.getGroup().put("packet", packetGroup);
        packetGroup.add(connReceiveMetric);
        packetGroup.add(connAckSentMetric);

        //消息数量
        List<MetricItemTO> messageGroup = new ArrayList<>();
        metricTO.getGroup().put("message", messageGroup);
        //消息分发
        List<MetricItemTO> deliveryGroup = new ArrayList<>();
        metricTO.getGroup().put("delivery", deliveryGroup);
    }

    @RequestMapping(OpenApi.DASHBOARD_OVERVIEW)
    public RestResult<OverViewTO> overview() {
        OverViewTO overViewTO = new OverViewTO();
        MetricTO metricTO = new MetricTO();
        Collection<MqttSession> sessions = brokerContext.getSessions();
        metricTO.setConnectCount(sessions.size());
        int subCount = 0;
        for (MqttSession session : sessions) {
            subCount += session.getSubscribers().size();
        }
        metricTO.setTopicCount(brokerContext.getTopics().size());
        metricTO.setSubscriberCount(subCount);
        overViewTO.setMetricTO(metricTO);

        overViewTO.setFlowInBytes(50 + (int) (Math.random() * 10));
        overViewTO.setFlowOutBytes(50 + (int) (Math.random() * 10));
        return RestResult.ok(overViewTO);
    }

    @RequestMapping(OpenApi.DASHBOARD_NODES)
    public RestResult<List<BrokerNodeTO>> nodes(HttpResponse response) {
        BrokerRuntime brokerRuntime = brokerContext.getRuntime();
        BrokerNodeTO nodeTO = new BrokerNodeTO();
        nodeTO.setNode("smart-mqtt@" + (StringUtils.isBlank(brokerContext.getBrokerConfigure().getHost()) ? "::1" : brokerContext.getBrokerConfigure().getHost()));
        nodeTO.setStatus(1);
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
        nodeTO.setMemory((int) ((systemMXBean.getTotalPhysicalMemorySize() + systemMXBean.getTotalSwapSpaceSize() - systemMXBean.getFreeSwapSpaceSize() - systemMXBean.getFreePhysicalMemorySize()) * 100.0 / (systemMXBean.getTotalPhysicalMemorySize() + systemMXBean.getTotalSwapSpaceSize())));
        return RestResult.ok(Arrays.asList(nodeTO));
    }

    @RequestMapping(OpenApi.DASHBOARD_METRICS)
    public RestResult<MetricTO> metrics() {
        return RestResult.ok(metricTO);
    }
}
