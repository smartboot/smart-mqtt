/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.exception.MqttException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/17
 */
public abstract class AsyncTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTask.class);

    @Override
    public final void run() {
        try {
            execute();
        } catch (MqttException e) {
            if (e.getCallback() != null) {
                e.getCallback().run();
            } else {
                LOGGER.error("execute async task exception", e);
            }
        } catch (Throwable throwable) {
            LOGGER.error("execute async task exception", throwable);
        }
    }

    public void execute() {

    }
}
