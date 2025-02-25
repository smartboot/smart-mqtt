package org.smartboot.mqtt.plugin.openapi.to;

public class TopicStatisticsTO {
    private String topic;
    private String clients;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getClients() {
        return clients;
    }

    public void setClients(String clients) {
        this.clients = clients;
    }

}
