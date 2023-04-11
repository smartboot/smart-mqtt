/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.message.MqttCodecUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/29
 */
public class MqttUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttUtil.class);
    /**
     * Topic 通配符
     */
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};

    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public static boolean containsTopicWildcards(String topicName) {
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String createClientId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getRemoteAddress(AbstractSession session) {
        try {
            return session.getRemoteAddress().toString();
        } catch (Exception e) {
            LOGGER.error("getRemoteAddress exception", e);
            return "";
        }
    }

    public static byte[] encodeCache(String topicName) {
        return cache.computeIfAbsent(topicName, s -> MqttCodecUtil.encodeUTF8(topicName));
    }
}
