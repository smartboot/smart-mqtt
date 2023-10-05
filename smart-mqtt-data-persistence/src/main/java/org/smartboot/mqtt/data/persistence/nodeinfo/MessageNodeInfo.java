package org.smartboot.mqtt.data.persistence.nodeinfo;

import com.alibaba.fastjson2.JSONObject;
import org.smartboot.mqtt.common.message.MqttPublishMessage;

import java.io.Serializable;

/**
* @Description: 持久化Vo对象
 * @Author: learnhope
 * @Date: 2023/9/18
 */
public class MessageNodeInfo implements Serializable {
    /**
     * 负载数据
     */
    private final byte[] payload;
    /**
     * 主题
     */
    private final String topic;
    
    private final boolean retained;
    
    /**
     * 消息存储时间
     */
    private final long createTime = System.currentTimeMillis();
    
    public MessageNodeInfo(MqttPublishMessage message) {
        this.payload = message.getPayload().getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = message.getVariableHeader().getTopicName();
    }
    
    public byte[] getPayload() {
        return payload;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public boolean isRetained() {
        return retained;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    

    
    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
    
}
