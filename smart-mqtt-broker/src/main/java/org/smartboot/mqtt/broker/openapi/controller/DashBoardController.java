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
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.BrokerNodeTO;
import org.smartboot.mqtt.broker.openapi.to.MetricTO;
import org.smartboot.mqtt.broker.openapi.to.OverViewTO;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
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

    public DashBoardController(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @RequestMapping(OpenApi.DASHBOARD_OVERVIEW)
    public RestResult<OverViewTO> overview(HttpResponse response) {
        OverViewTO overViewTO = new OverViewTO();
        MetricTO metricTO = new MetricTO();
        metricTO.setConnectCount(100 + (int) (Math.random() * 10));
        metricTO.setTopicCount(300 + (int) (Math.random() * 10));
        metricTO.setSubscriberCount(230 + (int) (Math.random() * 10));
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

}
