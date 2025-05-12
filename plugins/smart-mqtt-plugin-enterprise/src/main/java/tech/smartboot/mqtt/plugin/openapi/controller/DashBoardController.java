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

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/2
 */
@Controller
public class DashBoardController {

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private BrokerNodeMapper brokerNodeMapper;

//    @RequestMapping(OpenApi.DASHBOARD_NODES)
//    public RestResult<List<BrokerNodeTO>> nodes() {
//        List<BrokerNodeTO> list = NodeConvert.convert(brokerNodeMapper.selectAll());
//        list.stream().filter(brokerNodeTO -> BrokerStatueEnum.STOPPED.getCode().equals(brokerNodeTO.getStatus()) || BrokerStatueEnum.UNKNOWN.getCode().equals(brokerNodeTO.getStatus()))
//                .forEach(brokerNodeTO -> {
//                    brokerNodeTO.setRuntime("-");
//                    brokerNodeTO.setPid("-");
//                    brokerNodeTO.setMemory(0);
//                    brokerNodeTO.setCpu(0);
//                });
//        return RestResult.ok(list);
//    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setBrokerNodeMapper(BrokerNodeMapper brokerNodeMapper) {
        this.brokerNodeMapper = brokerNodeMapper;
    }
}
