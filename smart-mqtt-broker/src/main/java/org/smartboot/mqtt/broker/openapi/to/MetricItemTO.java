package org.smartboot.mqtt.broker.openapi.to;

import com.alibaba.fastjson2.annotation.JSONField;
import org.smartboot.mqtt.common.enums.MqttMetricEnum;

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
     * 指标值
     */
    private String value;

    @JSONField(serialize = false)
    private final LongAdder metric = new LongAdder();

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

    public String getValue() {
        return metric.toString();
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LongAdder getMetric() {
        return metric;
    }
}