package org.smartboot.mqtt.broker.openapi.controller;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.SubscriptionTO;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
@Controller
public class SubscriptionController {
    private final BrokerContext brokerContext;

    public SubscriptionController(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    @RequestMapping(OpenApi.SUBSCRIPTIONS_SUBSCRIPTION)
    public RestResult<List<SubscriptionTO>> subscription(HttpResponse response) {
        return RestResult.ok(null);
    }
}
