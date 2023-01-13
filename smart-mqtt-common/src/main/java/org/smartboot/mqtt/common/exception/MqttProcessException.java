package org.smartboot.mqtt.common.exception;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/23
 */
public class MqttProcessException extends RuntimeException {

    private static final long serialVersionUID = 2726736059967518427L;
    private Runnable callback;

    public MqttProcessException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public MqttProcessException(String showMessage, Runnable callback) {
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
