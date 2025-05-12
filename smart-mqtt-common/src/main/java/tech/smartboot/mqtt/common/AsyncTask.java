/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.common;

import tech.smartboot.mqtt.common.exception.MqttException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public abstract class AsyncTask implements Runnable {

    @Override
    public final void run() {
        try {
            execute();
        } catch (MqttException e) {
            if (e.getCallback() != null) {
                e.getCallback().run();
            } else {
                System.err.println("execute async task exception");
                e.getMessage();
            }
        } catch (Throwable throwable) {
            System.err.println("execute async task exception");
            throwable.getMessage();
        }
    }

    public void execute() {

    }
}
