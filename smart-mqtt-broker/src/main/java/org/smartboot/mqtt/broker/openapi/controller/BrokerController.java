package org.smartboot.mqtt.broker.openapi.controller;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.BrokerNodeTO;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 4/7/23
 */
@Controller
public class BrokerController {
    @RequestMapping(OpenApi.BROKERS)
    public RestResult<List<BrokerNodeTO>> brokers() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }
}
