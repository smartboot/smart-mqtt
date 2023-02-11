package org.smartboot.mqtt.broker.openapi.controller;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.Pagination;
import org.smartboot.mqtt.broker.openapi.to.SubscriptionTO;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
@Controller
public class SubscriptionController {

    @RequestMapping(OpenApi.SUBSCRIPTIONS_SUBSCRIPTION)
    public RestResult<Pagination<SubscriptionTO>> subscription() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }
}
