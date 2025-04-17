/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec.provider;


/**
 * 会话状态Provider
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/15
 */
public interface SessionStateProvider {
    /**
     * 存储会话状态
     */
    void store(String clientId, SessionState sessionState);

    /**
     * 获取指定clientId的会话状态
     */
    SessionState get(String clientId);

    /**
     * 清理会话状态
     */
    void remove(String clientId);
}
