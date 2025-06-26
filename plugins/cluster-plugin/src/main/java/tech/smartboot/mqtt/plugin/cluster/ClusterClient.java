package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.core.client.HttpClient;

class ClusterClient {
    HttpClient sseClient;

    HttpClient httpClient;

    boolean httpEnable;
    boolean sseEnable;

    boolean checkPending = false;
    final String baseURL;

    public ClusterClient(String url) {
        this.baseURL = url;
    }
}