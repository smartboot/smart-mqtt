package org.smartboot.mqtt.plugin.openapi.controller;

import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.plugin.convert.NodeConvert;
import org.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import org.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import org.smartboot.mqtt.plugin.openapi.OpenApi;
import org.smartboot.mqtt.plugin.openapi.to.BrokerNodeTO;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

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
