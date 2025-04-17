/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.util;

import org.apache.commons.lang.StringUtils;
import tech.smartboot.mqtt.common.exception.MqttException;

import java.util.Collection;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/23
 */
public class ValidateUtils {

    private static final Runnable DEFAULT_RUNNABLE = () -> {
    };

    public static void notBlank(String parameter, String message) {
        notBlank(parameter, message, DEFAULT_RUNNABLE);
    }

    public static void notBlank(String parameter, String message, Runnable callback) {
        if (StringUtils.isBlank(parameter)) {
            throwException(message, callback);
        }
    }

    public static void notNull(Object parameter, String message) {
        notNull(parameter, message, DEFAULT_RUNNABLE);
    }

    public static void notNull(Object parameter, String message, Runnable catchCallback) {
        if (parameter == null) {
            throwException(message, catchCallback);
        }
    }

    public static <E> void notEmpty(Collection<E> collection, String msg) {
        notEmpty(collection, msg, DEFAULT_RUNNABLE);
    }

    public static <E> void notEmpty(Collection<E> collection, String msg, Runnable callback) {
        if (collection == null || collection.isEmpty()) {
            throwException(msg, callback);
        }
    }

    public static <E> void isTrue(boolean flag, String msg) {
        isTrue(flag, msg, DEFAULT_RUNNABLE);
    }

    public static <E> void isTrue(boolean flag, String msg, Runnable callback) {
        if (!flag) {
            throwException(msg, callback);
        }
    }

    /**
     * 抛出异常信息
     */
    public static void throwException(String showMsg, Runnable runnable) {
        throw new MqttException(showMsg, runnable);
    }

    public static void throwException(String showMsg) {
        throw new MqttException(showMsg, DEFAULT_RUNNABLE);
    }
}
