package org.smartboot.mqtt.common;


import com.alibaba.fastjson2.JSONObject;

/**
 * @author 三刀
 * @version V1.0 , 2018/5/3
 */
public class ToString {
    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
