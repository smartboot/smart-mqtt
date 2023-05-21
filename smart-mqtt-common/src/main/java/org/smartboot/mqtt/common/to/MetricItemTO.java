/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.to;

import com.alibaba.fastjson2.annotation.JSONField;
import org.smartboot.mqtt.common.enums.MqttMetricEnum;

import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/26
 */
public class MetricItemTO {
    /**
     * 指标编码
     */
    private String code;
    /**
     * 指标描述
     */
    private String desc;

    /**
     * 指标数据
     */
    @JSONField(serialize = false)
    private final LongAdder metric = new LongAdder();

    /**
     * 上一次数据
     */
    private int latestValue;

    /**
     * 采集周期，单位：秒，非正整数表示禁用周期统计
     */
    private final int period;
    /**
     * 未启用周期采集改值为null
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date time;


    public MetricItemTO() {
        this.period = 0;
    }

    public MetricItemTO(MqttMetricEnum metricEnum) {
        this(metricEnum, 0);
    }

    public MetricItemTO(MqttMetricEnum metricEnum, int period) {
        this.code = metricEnum.getCode();
        this.desc = metricEnum.getDesc();
        this.period = period;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getValue() {
        return metric.intValue();
    }

    public void setValue(int value) {
        metric.reset();
        metric.add(value);
    }

    public LongAdder getMetric() {
        return metric;
    }

    public int getPeriod() {
        return period;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getLatestValue() {
        return latestValue;
    }

    public void setLatestValue(int latestValue) {
        this.latestValue = latestValue;
    }
}
