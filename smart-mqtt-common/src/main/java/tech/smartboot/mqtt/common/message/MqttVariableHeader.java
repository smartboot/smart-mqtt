/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.message;

import tech.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

/**
 * 可变报头
 *
 * @author 三刀(zhengjunweimail @ 163.com)
 */
public abstract class MqttVariableHeader<T extends AbstractProperties> extends Codec {
    protected final T properties;

    public MqttVariableHeader(T properties) {
        this.properties = properties;
    }

    public final T getProperties() {
        return properties;
    }

    @Override
    protected final int preEncode() {
        int length = preEncode0();
        if (properties != null) {
            length += properties.preEncode();
        }
        return length;
    }

    protected abstract int preEncode0();

}
