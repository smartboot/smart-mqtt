/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.eventbus.messagebus;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.ToString;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.enums.PayloadEncodeEnum;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.util.Base64;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public class Message extends ToString {
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
     * 存储的消息偏移量
     */
    private long offset;

    private final MqttQoS qos;
    /**
     * 消息存储时间
     */
    private final long createTime = MqttUtil.currentTimeMillis();

    private final String clientId;

    Message(AbstractSession session, MqttPublishMessage message) {
        this.clientId = session.getClientId();
        this.payload = message.getPayload().getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = message.getVariableHeader().getTopicName();
        this.qos = message.getFixedHeader().getQosLevel();
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

    public long getOffset() {
        return offset;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getClientId() {
        return clientId;
    }

    @JSONField(serialize = false)
    private String defaultJson;
    @JSONField(serialize = false)
    private String stringJson;
    @JSONField(serialize = false)
    private String base64Json;

    public String getJsonObject(PayloadEncodeEnum payloadEncodeEnum) {
        if (payloadEncodeEnum == null) {
            payloadEncodeEnum = PayloadEncodeEnum.BYTES;
        }
        switch (payloadEncodeEnum) {
            case STRING:
                if (stringJson == null) {
                    JSONObject json = (JSONObject) JSON.toJSON(this);
                    json.put("payload", new String(payload));
                    json.put("encoding", payloadEncodeEnum.getCode());
                    stringJson = json.toString();
                }
                return stringJson;
            case BASE64:
                if (base64Json == null) {
                    JSONObject json = (JSONObject) JSON.toJSON(this);
                    json.put("payload", new String(Base64.getEncoder().encode(payload)));
                    json.put("encoding", payloadEncodeEnum.getCode());
                    base64Json = json.toString();
                }
                return base64Json;
            default:
                if (defaultJson == null) {
                    JSONObject json = (JSONObject) JSON.toJSON(this);
                    json.put("encoding", payloadEncodeEnum.getCode());
                    defaultJson = json.toString();
                }
                return defaultJson;
        }
    }
}
