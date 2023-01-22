package org.smartboot.mqtt.broker.openapi.dashboard;

import com.sun.management.OperatingSystemMXBean;
import jdk.jfr.internal.JVM;
import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.openapi.OpenApi;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
@Controller
public class DashBoardController {


    @RequestMapping(OpenApi.DASHBOARD_OVERVIEW)
    public RestResult<OverViewTO> overview(HttpResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
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
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");

        BrokerNodeTO nodeTO = new BrokerNodeTO();
        nodeTO.setNode("smart-mqtt@192.168.1.3");
        nodeTO.setStatus(1);
        nodeTO.setVersion(BrokerConfigure.VERSION);
        nodeTO.setPid(JVM.getJVM().getPid());

        OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        nodeTO.setCpu((int) (systemMXBean.getSystemCpuLoad() * 100));
        nodeTO.setMemory((int) ((systemMXBean.getTotalPhysicalMemorySize()+systemMXBean.getTotalSwapSpaceSize()-systemMXBean.getFreeSwapSpaceSize() - systemMXBean.getFreePhysicalMemorySize()) * 100.0 / (systemMXBean.getTotalPhysicalMemorySize()+systemMXBean.getTotalSwapSpaceSize())));
        return RestResult.ok(Arrays.asList(nodeTO));
    }

}
