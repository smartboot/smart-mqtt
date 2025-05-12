package tech.smartboot.mqtt.tls;

/**
 * @author 三刀
 * @version v1.0 5/7/25
 */
public class PluginConfig {
    private int port;
    private String host;

    private String pem;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
