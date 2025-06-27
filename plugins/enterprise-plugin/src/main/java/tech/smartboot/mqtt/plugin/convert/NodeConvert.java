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
import tech.smartboot.mqtt.plugin.cluster.NodeProcessInfo;
import tech.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import tech.smartboot.mqtt.plugin.openapi.to.BrokerNodeTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/4
 */
public class NodeConvert {
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * 60;
    private static final int DAY = 24 * 60 * 60;

    public static List<BrokerNodeTO> convert(List<BrokerNodeDO> nodeDOS) {
        if (nodeDOS == null || nodeDOS.isEmpty()) {
            return Collections.emptyList();
        }
        List<BrokerNodeTO> list = new ArrayList<>(nodeDOS.size());
        nodeDOS.forEach(brokerNodeDO -> list.add(convert(brokerNodeDO)));
        return list;
    }

    public static BrokerNodeTO convert(BrokerNodeDO nodeDO) {
        if (nodeDO == null) {
            return null;
        }
        BrokerNodeTO brokerNodeTO = new BrokerNodeTO();
        brokerNodeTO.setNodeId(nodeDO.getNodeId());
        brokerNodeTO.setLocalAddress(nodeDO.getNodeId());
        brokerNodeTO.setStatus(nodeDO.getStatus());
        brokerNodeTO.setIpAddress(nodeDO.getIpAddress());
        brokerNodeTO.setPort(nodeDO.getPort());
        brokerNodeTO.setRuntime(getRuntime(nodeDO.getStartTime().getTime()));
        brokerNodeTO.setStartTime(nodeDO.getStartTime().getTime());
        brokerNodeTO.setNodeType(nodeDO.getNodeType());
        brokerNodeTO.setCoreNodeId(nodeDO.getCoreNodeId());
        brokerNodeTO.setClusterEndpoint(nodeDO.getClusterEndpoint());

        NodeProcessInfo process = JSONObject.parseObject(nodeDO.getProcess(), NodeProcessInfo.class);
        if (process != null) {
            brokerNodeTO.setVersion(process.getVersion());
            brokerNodeTO.setPid(process.getPid());
            brokerNodeTO.setVmVendor(process.getVmVendor());
            brokerNodeTO.setVmVersion(process.getVmVersion());
            brokerNodeTO.setOsVersion(process.getOsVersion());
            brokerNodeTO.setOsArch(process.getOsArch());
            brokerNodeTO.setOsName(process.getOsName());
            brokerNodeTO.setMemUsage(process.getMemUsage());
            brokerNodeTO.setMemoryLimit(process.getMemoryLimit());
            brokerNodeTO.setCpuUsage(process.getCpuUsage());
        }
        return brokerNodeTO;
    }

    private static String getRuntime(long startTime) {
        //运行时长
        StringBuilder sb = new StringBuilder();
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        if (runtime >= DAY) {
            sb.append(runtime / DAY).append("天");
            runtime %= DAY;
        }
        if (runtime >= HOUR) {
            sb.append(runtime / HOUR).append("小时");
            runtime %= MINUTE;
        }
        if (runtime >= MINUTE) {
            sb.append(runtime / MINUTE).append("分");
        }
        sb.append(runtime % 60).append("秒");
        return sb.toString();
    }
}
