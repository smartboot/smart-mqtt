package org.smartboot.mqtt.broker.plugins;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class PluginException extends RuntimeException {
    public PluginException(String message) {
        super(message);
    }
}
