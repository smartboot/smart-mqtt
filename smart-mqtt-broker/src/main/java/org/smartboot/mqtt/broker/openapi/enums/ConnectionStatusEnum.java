package org.smartboot.mqtt.broker.openapi.enums;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
public enum ConnectionStatusEnum {
    /**
     * 已连接
     */
    CONNECTED("connected"),
    /**
     * 已断开
     */
    DIS_CONNECT("disconnect");
    private final String status;

    ConnectionStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
