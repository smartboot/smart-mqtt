/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.convert;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.mqtt.plugin.dao.model.PluginConfigDO;
import tech.smartboot.mqtt.plugin.openapi.to.BridgeConfigTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BridgeConvert {
    public static BridgeConfigTO convert(PluginConfigDO configDO) {
        if (configDO == null) {
            return null;
        }
        BridgeConfigTO bridgeConfigTO = new BridgeConfigTO();
        bridgeConfigTO.setId(configDO.getId());
        bridgeConfigTO.setType(configDO.getPluginType());
        bridgeConfigTO.setConfig(JSONObject.parseObject(configDO.getConfig()));
        bridgeConfigTO.setStatus(configDO.getStatus());
        return bridgeConfigTO;
    }

    public static List<BridgeConfigTO> convert(List<PluginConfigDO> configDOS) {
        if (configDOS == null || configDOS.isEmpty()) {
            return Collections.emptyList();
        }
        List<BridgeConfigTO> list = new ArrayList<>(configDOS.size());
        configDOS.forEach(configDO -> list.add(convert(configDO)));
        return list;
    }
}
