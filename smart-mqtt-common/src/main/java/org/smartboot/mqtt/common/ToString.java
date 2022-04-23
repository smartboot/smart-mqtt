package org.smartboot.mqtt.common;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class ToString {
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
