package org.smartboot.mqtt.plugin.openapi.to;

public class SettingsTO {

    private String metricRecord;
    private String connectRecord;
    private String subscribeRecord;

    private String showMetrics;

    public String getConnectRecord() {
        return connectRecord;
    }

    public void setConnectRecord(String connectRecord) {
        this.connectRecord = connectRecord;
    }

    public String getSubscribeRecord() {
        return subscribeRecord;
    }

    public void setSubscribeRecord(String subscribeRecord) {
        this.subscribeRecord = subscribeRecord;
    }

    public String getMetricRecord() {
        return metricRecord;
    }

    public void setMetricRecord(String metricRecord) {
        this.metricRecord = metricRecord;
    }

    public String getShowMetrics() {
        return showMetrics;
    }

    public void setShowMetrics(String showMetrics) {
        this.showMetrics = showMetrics;
    }
}
