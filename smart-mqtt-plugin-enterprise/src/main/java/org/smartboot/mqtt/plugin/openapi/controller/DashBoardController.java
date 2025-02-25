package org.smartboot.mqtt.plugin.openapi.controller;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/2
 */
@Controller(async = true)
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
