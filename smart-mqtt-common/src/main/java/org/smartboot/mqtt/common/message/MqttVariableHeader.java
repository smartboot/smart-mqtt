package org.smartboot.mqtt.common.message;

import org.smartboot.mqtt.common.message.variable.properties.AbstractProperties;

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
