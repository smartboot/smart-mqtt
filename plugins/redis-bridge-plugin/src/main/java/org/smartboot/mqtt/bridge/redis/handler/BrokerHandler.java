/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.bridge.redis.handler;

import com.sun.management.OperatingSystemMXBean;
import org.smartboot.mqtt.bridge.redis.nodeinfo.BrokerNodeInfo;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class BrokerHandler {

    private static final String MQTT_PREFIX = "smart-mqtt@";


    public static Map<String, String> handler(BrokerContext brokerContext) {
        BrokerNodeInfo brokerNodeInfo = new BrokerNodeInfo();
        // 名字设置
        brokerNodeInfo.setName(MQTT_PREFIX + brokerContext.getBrokerConfigure().getNodeId());
        // 设置Broker版本号
        brokerNodeInfo.setVersion(BrokerConfigure.VERSION);
        // 设置ip地址
        brokerNodeInfo.setIpAddress(brokerContext.getBrokerConfigure().getHost());
        // 设置内存
        brokerNodeInfo.setMemory(String.valueOf((int) ((Runtime.getRuntime().totalMemory()) * 100.0 / (Runtime.getRuntime().maxMemory()))));
        // 设置cpu资源
        OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        brokerNodeInfo.setCpu(String.valueOf((int) (systemMXBean.getSystemCpuLoad() * 100)));
        // 最后一次启动时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeString = dateFormat.format(new Date());
        brokerNodeInfo.setRecentTime(currentDateTimeString);

        return brokerNodeInfo.toMap();
    }
}
