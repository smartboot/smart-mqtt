/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.message.variable;

import org.smartboot.mqtt.common.message.MqttVariableHeader;
import org.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/23
 */
public abstract class MqttPacketIdVariableHeader<T extends AbstractProperties> extends MqttVariableHeader<T> {
    /**
     * 报文标识符
     */
    private int packetId;

    public MqttPacketIdVariableHeader(int packetId, T properties) {
        super(properties);
        this.packetId = packetId;
    }


    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }
}
