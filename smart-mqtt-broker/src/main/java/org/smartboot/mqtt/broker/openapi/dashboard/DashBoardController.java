package org.smartboot.mqtt.broker.openapi.dashboard;

import jdk.jfr.internal.JVM;
import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.openapi.OpenApi;

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

        BrokerNodeTO brokerNodeTO = new BrokerNodeTO();
        brokerNodeTO.setNode("smart-mqtt@192.168.1.1");
        brokerNodeTO.setStatus(1);
        brokerNodeTO.setVersion(BrokerConfigure.VERSION);
        brokerNodeTO.setPid(JVM.getJVM().getPid());
        brokerNodeTO.setCpu(50 + (int) (Math.random() * 50));
        brokerNodeTO.setMemory(50 + (int) (Math.random() * 50));

        BrokerNodeTO brokerNodeTO2 = new BrokerNodeTO();
        brokerNodeTO2.setNode("smart-mqtt@192.168.1.2");
        brokerNodeTO2.setStatus(2);
        brokerNodeTO2.setVersion(BrokerConfigure.VERSION);
        brokerNodeTO2.setPid(JVM.getJVM().getPid());
        brokerNodeTO2.setCpu(50 + (int) (Math.random() * 50));
        brokerNodeTO2.setMemory(50 + (int) (Math.random() * 50));

        BrokerNodeTO brokerNodeTO3 = new BrokerNodeTO();
        brokerNodeTO3.setNode("smart-mqtt@192.168.1.3");
        brokerNodeTO3.setStatus(1);
        brokerNodeTO3.setVersion(BrokerConfigure.VERSION);
        brokerNodeTO3.setPid(JVM.getJVM().getPid());
        brokerNodeTO3.setCpu(50 + (int) (Math.random() * 50));
        brokerNodeTO3.setMemory(50 + (int) (Math.random() * 50));

//        OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        return RestResult.ok(Arrays.asList(brokerNodeTO, brokerNodeTO2, brokerNodeTO3));
    }

}
