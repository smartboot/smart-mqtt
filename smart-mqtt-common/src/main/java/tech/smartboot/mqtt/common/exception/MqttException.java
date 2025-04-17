/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common.exception;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/23
 */
public class MqttException extends RuntimeException {
    private static final Runnable DEFAULT_RUNNABLE = () -> {
    };
    private static final long serialVersionUID = 2726736059967518427L;
    private final Runnable callback;

    public MqttException(String message, Throwable cause) {
        super(message, cause);
        this.callback = DEFAULT_RUNNABLE;
    }

    public MqttException(String showMessage, Runnable callback) {
        super(showMessage);
        this.callback = callback;
    }

    /**
     * 异常不打异常堆栈
     */
    @Override
    public Throwable fillInStackTrace() {
        return super.fillInStackTrace();
    }

    public Runnable getCallback() {
        return callback;
    }
}
