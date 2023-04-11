/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
