/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.enums;

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
