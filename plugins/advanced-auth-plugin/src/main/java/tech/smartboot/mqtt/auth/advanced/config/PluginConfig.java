/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced.config;

import java.util.List;

/**
 * 插件配置类
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class PluginConfig {

    /**
     * 认证失败时是否立即停止
     * <p>
     * - true: 当任一认证器验证失败时，立即拒绝连接，不再尝试后续认证器
     * - false: 即使某个认证器失败，也会继续尝试认证链中的下一个认证器
     * <p>
     * 适用场景：
     * - true: 适用于严格认证场景，快速失败，减少无效尝试
     * - false: 适用于多认证源容错场景，提高认证成功率
     */
    private boolean stopOnError = true;
    
    /**
     * 是否允许匿名访问
     * <p>
     * - true: 允许客户端不提供用户名和密码直接连接
     * - false: 客户端必须提供用户名和密码才能连接
     * <p>
     * 安全建议：
     * - 开发测试环境：可开启，方便调试
     * - 生产环境：建议关闭，确保连接安全性
     * - 如开启，建议配合 ACL 插件限制匿名用户的发布/订阅权限
     */
    private boolean allowAnonymous = false;

    /**
     * Redis 认证器配置
     */
    private RedisConfig redis;


    /**
     * HTTP 认证器配置
     */
    private HttpConfig http;

    /**
     * 认证链顺序配置
     * <p>
     * 定义认证器的执行顺序，支持以下认证器：
     * - redis: Redis 认证器，从 Redis 查询用户凭证进行认证
     * - http: HTTP 认证器，调用外部 HTTP 接口进行认证
     * - mysql: MySQL 认证器（暂未实现）
     * <p>
     * 执行规则：
     * - 按数组顺序依次执行认证器
     * - 如果某个认证器返回 SUCCESS，则认证成功，停止后续认证
     * - 如果某个认证器返回 FAILURE，且 stopOnError=true，则立即拒绝连接
     * - 如果某个认证器返回 CONTINUE 或异常，则继续尝试下一个认证器
     * <p>
     * 示例配置：
     * <pre>{@code
     * chain:
     *   - redis  # 优先使用 Redis 认证
     *   - http   # Redis 未找到用户时，使用 HTTP 认证
     * }</pre>
     */
    private List<String> chain;

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }


    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }


}
