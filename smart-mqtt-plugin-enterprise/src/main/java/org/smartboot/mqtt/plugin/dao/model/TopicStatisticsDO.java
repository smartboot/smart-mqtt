package org.smartboot.mqtt.plugin.dao.model;

public class TopicStatisticsDO {
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
