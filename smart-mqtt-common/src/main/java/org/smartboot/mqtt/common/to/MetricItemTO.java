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
     * 未启用周期采集改值为null
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date time;


    public MetricItemTO() {

    }

    public MetricItemTO(MqttMetricEnum metricEnum) {
        this.code = metricEnum.getCode();
        this.desc = metricEnum.getDesc();
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

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
