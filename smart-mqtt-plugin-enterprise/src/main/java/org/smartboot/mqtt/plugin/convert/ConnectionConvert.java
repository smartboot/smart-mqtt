package org.smartboot.mqtt.plugin.convert;

import org.smartboot.mqtt.plugin.dao.model.ConnectionDO;
import org.smartboot.mqtt.plugin.openapi.to.ConnectionTO;

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
