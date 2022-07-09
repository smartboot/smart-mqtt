package org.smartboot.mqtt.common;


import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class ToString {
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
