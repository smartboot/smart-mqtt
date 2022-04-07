package org.smartboot.mqtt.client;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/7
 */
public interface Callback<T> {
    void onSuccess(T t);

    void onFailure(Throwable throwable);
}
