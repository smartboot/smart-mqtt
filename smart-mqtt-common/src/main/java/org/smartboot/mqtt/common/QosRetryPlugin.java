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

import org.smartboot.mqtt.common.message.MqttMessage;
import org.smartboot.socket.extension.plugins.AbstractPlugin;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.Attachment;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/3/24
 */
public class QosRetryPlugin extends AbstractPlugin<MqttMessage> {

    @Override
    public void beforeRead(AioSession session) {
        Attachment attachment = session.getAttachment();
        if (attachment == null) {
            return;
        }
        Runnable runnable = attachment.get(InflightQueue.RETRY_TASK_ATTACH_KEY);
        if (runnable != null) {
            try {
                runnable.run();
            } finally {
                attachment.remove(InflightQueue.RETRY_TASK_ATTACH_KEY);
            }
        }
    }
}
