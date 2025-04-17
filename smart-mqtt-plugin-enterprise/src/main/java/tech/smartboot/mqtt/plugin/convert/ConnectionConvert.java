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

import tech.smartboot.mqtt.plugin.dao.model.ConnectionDO;
import tech.smartboot.mqtt.plugin.openapi.to.ConnectionTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
public class ConnectionConvert {
    public static ConnectionTO convert(ConnectionDO connectionDO) {
        if (connectionDO == null) {
            return null;
        }
        ConnectionTO connectionTO = new ConnectionTO();
        connectionTO.setClientId(connectionDO.getClientId());
        connectionTO.setUsername(connectionDO.getUsername());
        connectionTO.setStatus(connectionDO.getStatus());
        connectionTO.setIpAddress(connectionDO.getIpAddress());
        connectionTO.setNodeId(connectionDO.getNodeId());
        connectionTO.setKeepalive(connectionDO.getKeepalive());
        connectionTO.setConnectTime(connectionDO.getConnectTime());
        return connectionTO;
    }

    public static List<ConnectionTO> convert(List<ConnectionDO> doList) {
        if (doList == null || doList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConnectionTO> list = new ArrayList<>(doList.size());
        doList.forEach(connectionDO -> list.add(convert(connectionDO)));
        return list;
    }
}
