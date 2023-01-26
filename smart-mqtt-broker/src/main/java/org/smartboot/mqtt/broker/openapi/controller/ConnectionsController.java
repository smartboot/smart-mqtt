package org.smartboot.mqtt.broker.openapi.controller;

import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.to.ConnectionTO;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
@Controller
public class ConnectionsController {

    @RequestMapping(OpenApi.CONNECTIONS)
    public RestResult<List<ConnectionTO>> connections() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }
}
