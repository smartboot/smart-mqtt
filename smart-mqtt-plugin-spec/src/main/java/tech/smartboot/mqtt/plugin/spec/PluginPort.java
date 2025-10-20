package tech.smartboot.mqtt.plugin.spec;

/**
 * @author 三刀
 * @version v1.0 10/20/25
 */
public class PluginPort {
    private final int port;
    private final String desc;

    public PluginPort(int port, String desc) {
        this.port = port;
        this.desc = desc;
    }

    public int getPort() {
        return port;
    }

    public String getDesc() {
        return desc;
    }
}
