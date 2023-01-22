package org.smartboot.mqtt.broker.openapi.dashboard;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OverViewTO {
    private MetricTO metricTO;

    private int flowInBytes;

    private int flowOutBytes;

    public MetricTO getMetricTO() {
        return metricTO;
    }

    public void setMetricTO(MetricTO metricTO) {
        this.metricTO = metricTO;
    }

    public int getFlowInBytes() {
        return flowInBytes;
    }

    public void setFlowInBytes(int flowInBytes) {
        this.flowInBytes = flowInBytes;
    }

    public int getFlowOutBytes() {
        return flowOutBytes;
    }

    public void setFlowOutBytes(int flowOutBytes) {
        this.flowOutBytes = flowOutBytes;
    }
}
