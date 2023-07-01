/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.openapi.controller;

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.restful.RestResult;
import org.smartboot.http.restful.annotation.Autowired;
import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.BrokerRuntime;
import org.smartboot.mqtt.broker.openapi.OpenApi;
import org.smartboot.mqtt.broker.openapi.enums.BrokerStatueEnum;
import org.smartboot.mqtt.broker.openapi.to.BrokerNodeTO;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
@Controller
public class DashBoardController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashBoardController.class);
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * 60;
    private static final int DAY = 24 * 60 * 60;
    @Autowired
    private BrokerContext brokerContext;


    @RequestMapping(OpenApi.DASHBOARD_OVERVIEW)
    public RestResult<?> overview() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }

    @RequestMapping(OpenApi.DASHBOARD_NODES)
    public RestResult<List<BrokerNodeTO>> nodes() {
        BrokerRuntime brokerRuntime = brokerContext.getRuntime();
        BrokerNodeTO nodeTO = new BrokerNodeTO();
        nodeTO.setName("smart-mqtt@" + (StringUtils.isBlank(brokerContext.getBrokerConfigure().getHost()) ? "::1" : brokerContext.getBrokerConfigure().getHost()));
        nodeTO.setStatus(BrokerStatueEnum.RUNNING.getCode());
        nodeTO.setVersion(BrokerConfigure.VERSION);
        nodeTO.setPid(brokerRuntime.getPid());

        //运行时长
        StringBuilder sb = new StringBuilder();
        long runtime = (System.currentTimeMillis() - brokerRuntime.getStartTime()) / 1000;
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
        nodeTO.setRuntime(sb.toString());


        OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        nodeTO.setCpu((int) (systemMXBean.getSystemCpuLoad() * 100));
        nodeTO.setMemory((int) ((Runtime.getRuntime().totalMemory()) * 100.0 / (Runtime.getRuntime().maxMemory())));
        return RestResult.ok(Arrays.asList(nodeTO));
    }

    /**
     * 指标信息
     *
     * @return
     */
    @RequestMapping(OpenApi.DASHBOARD_METRICS)
    public RestResult<?> metrics() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }


    /**
     * 指标信息
     *
     * @return
     */
    @RequestMapping(OpenApi.DASHBOARD_CLUSTER)
    public RestResult<?> cluster() {
        return RestResult.fail(OpenApi.MESSAGE_UPGRADE);
    }

}
