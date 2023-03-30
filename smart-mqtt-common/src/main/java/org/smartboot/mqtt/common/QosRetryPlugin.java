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
