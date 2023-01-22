package org.smartboot.mqtt.broker.openapi.dashboard;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.openapi.OpenApi;

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
}
