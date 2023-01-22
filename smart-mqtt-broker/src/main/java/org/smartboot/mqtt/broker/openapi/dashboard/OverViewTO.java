package org.smartboot.mqtt.broker.openapi.dashboard;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OverViewTO {
    private MetricTO metricTO;

    public MetricTO getMetricTO() {
        return metricTO;
    }

    public void setMetricTO(MetricTO metricTO) {
        this.metricTO = metricTO;
    }
}
