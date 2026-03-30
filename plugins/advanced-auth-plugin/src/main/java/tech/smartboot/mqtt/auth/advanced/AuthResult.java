/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced;

/**
 * 认证结果枚举
 * 
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public enum AuthResult {
    /**
     * 认证成功，停止后续认证
     */
    SUCCESS,
    
    /**
     * 认证失败但继续下一个认证器（当前认证器无法处理该请求）
     */
    CONTINUE,
    
    /**
     * 认证失败，终止连接
     */
    FAILURE,
}
