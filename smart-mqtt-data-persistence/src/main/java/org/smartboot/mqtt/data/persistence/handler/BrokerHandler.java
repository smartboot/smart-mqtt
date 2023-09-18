package org.smartboot.mqtt.data.persistence.handler;

import com.sun.management.OperatingSystemMXBean;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerRuntime;
import org.smartboot.mqtt.data.persistence.nodeinfo.BrokerNodeInfo;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class BrokerHandler {
    public static Map<String,String> handler(BrokerContext brokerContext){
        BrokerRuntime brokerRuntime = brokerContext.getRuntime();
        BrokerNodeInfo brokerNodeInfo = new BrokerNodeInfo();
        // 名字设置
        brokerNodeInfo.setName(brokerContext.getBrokerConfigure().getName());
        // 设置Broker版本号
        brokerNodeInfo.setVersion(BrokerConfigure.VERSION);
        // 设置ip地址
        brokerNodeInfo.setIpAddress(brokerContext.getBrokerConfigure().getHost());
        // 设置Pid
        brokerNodeInfo.setPid(brokerRuntime.getPid());
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
