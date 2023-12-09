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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.AbstractSession;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.message.MqttCodecUtil;
import org.smartboot.socket.timer.HashedWheelTimer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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


    /**
     * 当前时间
     */
    private static long currentTimeMillis = System.currentTimeMillis();

    static {
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> currentTimeMillis = System.currentTimeMillis(), 1, TimeUnit.SECONDS);
    }

    public static long currentTimeMillis() {
        return currentTimeMillis;
    }

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

    public static boolean match(TopicToken pubTopicToken, TopicToken subTopicToken) {
        if (subTopicToken == null) {
            return pubTopicToken == null;
        }
        //合法的#通配符必然存在于末端
        if ("#".equals(subTopicToken.getNode())) {
            return true;
        }
        if ("+".equals(subTopicToken.getNode())) {
            return pubTopicToken != null && match(pubTopicToken.getNextNode(), subTopicToken.getNextNode());
        }
        if (pubTopicToken == null || !StringUtils.equals(pubTopicToken.getNode(), subTopicToken.getNode())) {
            return false;
        }
        return match(pubTopicToken.getNextNode(), subTopicToken.getNextNode());
    }

    public static void updateConfig(Object config, String prefix) {
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                //系统属性优先
                String v = System.getProperty(prefix + "." + field.getName());
                //环境属性次之
                if (v == null) {
                    v = System.getenv((prefix + "." + field.getName()).replace(".", "_").toUpperCase());
                }
                if (v == null) {
                    continue;
                }
                Class<?> type = field.getType();
                if (type == int.class) {
                    field.set(config, Integer.parseInt(v));
                } else if (type == String.class) {
                    field.set(config, v);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        } catch (Throwable throwable) {
            throw new MqttException("update config exception", throwable);
        }

    }
}
