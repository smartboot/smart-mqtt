package org.smartboot.mqtt.broker;

/**
 * broker服务配置
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/30
 */
public class BrokerConfigure {
    /**
     * 端口号
     */
    private int port;

    /**
     * 默认的客户端keep-alive超时时间.
     * 保持连接的实际值是由应用指定的，一般是几分钟。允许的最大值是 18 小时 12 分 15 秒。
     */
    private long maxKeepAliveTime = 10000;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getMaxKeepAliveTime() {
        return maxKeepAliveTime;
    }

    public void setMaxKeepAliveTime(long maxKeepAliveTime) {
        this.maxKeepAliveTime = maxKeepAliveTime;
    }
}
