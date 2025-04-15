/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.plugin.spec.bus;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/17
 */
public abstract class DisposableEventBusSubscriber<T> implements EventBusSubscriber<T> {
    private boolean enabled = true;

    @Override
    public final boolean enable() {
        try {
            return enabled;
        } finally {
            enabled = false;
        }

    }
}
