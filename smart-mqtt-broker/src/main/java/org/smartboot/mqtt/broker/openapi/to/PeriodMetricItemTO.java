package org.smartboot.mqtt.broker.openapi.to;

import com.alibaba.fastjson2.annotation.JSONField;
import org.smartboot.mqtt.common.enums.MqttPeriodMetricEnum;

import java.util.Date;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/26
 */
public class PeriodMetricItemTO extends MetricItemTO {
    /**
     * 采集周期，单位：秒
     */
    private int period;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    public PeriodMetricItemTO() {
    }

    public PeriodMetricItemTO(MqttPeriodMetricEnum metricEnum) {
        setCode(metricEnum.getCode());
        setDesc(metricEnum.getDesc());
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
