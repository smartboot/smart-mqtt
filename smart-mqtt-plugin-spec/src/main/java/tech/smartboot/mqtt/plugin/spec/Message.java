/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.mqtt.common.ToString;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.PayloadEncodeEnum;
import tech.smartboot.mqtt.common.message.MqttPublishMessage;
import tech.smartboot.mqtt.common.util.MqttUtil;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/24
 */
public final class Message extends ToString {
    /**
     * 负载数据
     */
    private final byte[] payload;

    /**
     * 主题
     */
    private final BrokerTopic topic;

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

    /**
     * 本条消息可推送的次数
     */
    private AtomicInteger pushSemaphore;

    public Message(MqttPublishMessage message, BrokerTopic topic) {
        this.payload = message.getPayload().getPayload();
        this.retained = message.getFixedHeader().isRetain();
        this.topic = topic;
        this.qos = message.getFixedHeader().getQosLevel();
    }

    public Message(BrokerTopic topic, MqttQoS qos, byte[] message, boolean retained) {
        this.payload = message;
        this.retained = retained;
        this.topic = topic;
        this.qos = qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public BrokerTopic getTopic() {
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

    public int decrementAndGet() {
        return pushSemaphore.decrementAndGet();
    }

    public void setPushSemaphore(int pushSemaphore) {
        this.pushSemaphore = new AtomicInteger(pushSemaphore);
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
