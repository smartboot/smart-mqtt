/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.to;

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
