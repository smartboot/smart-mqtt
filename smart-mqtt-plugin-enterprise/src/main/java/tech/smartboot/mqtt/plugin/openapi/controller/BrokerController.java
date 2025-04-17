/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.mqtt.plugin.convert.NodeConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import tech.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.BrokerNodeTO;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 4/7/23
 */
@Controller(async = true)
public class BrokerController {

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private BrokerNodeMapper brokerNodeMapper;

    @RequestMapping(OpenApi.BROKERS)
    public RestResult<List<BrokerNodeTO>> brokers() {
        List<BrokerNodeDO> list = brokerNodeMapper.selectAll();

        return RestResult.ok(NodeConvert.convert(list));
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setBrokerNodeMapper(BrokerNodeMapper brokerNodeMapper) {
        this.brokerNodeMapper = brokerNodeMapper;
    }
}
