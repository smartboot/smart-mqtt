/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.client.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.client.MqttClient;
import org.smartboot.mqtt.common.message.MqttPingRespMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/12
 */
public class MqttPingRespProcessor implements MqttProcessor<MqttPingRespMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPingRespProcessor.class);

    @Override
    public void process(MqttClient mqttClient, MqttPingRespMessage message) {
//        LOGGER.info("receive ping response");
    }
}
