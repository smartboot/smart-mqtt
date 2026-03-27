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
     * 认证器出错时是否停止（默认true）
     */
    private boolean stopOnError = true;

    /**
     * 是否启用匿名访问（默认false）
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
     * 认证链顺序（按此顺序执行认证器，默认按 redis -> mysql -> http）
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

    /**
     * 获取认证链顺序
     * 如果未配置，则默认返回 redis -> mysql -> html
     */
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }


}
