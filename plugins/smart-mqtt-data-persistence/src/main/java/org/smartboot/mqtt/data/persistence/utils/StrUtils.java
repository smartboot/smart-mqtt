package org.smartboot.mqtt.data.persistence.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.Base64;

/**
* @Description: 对字符串进行base64编码
 * @Author: learnhope
 * @Date: 2023/9/18
 */
public class StrUtils<T> {
    public String base64(T t){
        String jsonString = JSON.toJSONString(t);
        JSONObject json = JSONObject.parseObject(jsonString);
        // 对payload进行base64编码
        String payload = json.getString("payload");
        String encodedPayload = Base64.getEncoder().encodeToString(payload.getBytes());
        json.put("payload", encodedPayload);
        return json.toJSONString();
    }
    
    public static String addId(String jsonString){
        JSONObject json = JSONObject.parseObject(jsonString);
        json.put("randomId", IdUtils.getIdStr());
        return json.toJSONString();
    }
}
