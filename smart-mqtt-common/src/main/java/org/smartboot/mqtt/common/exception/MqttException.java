package org.smartboot.mqtt.common.exception;

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
