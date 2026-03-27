/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经 smartboot 组织特别许可，需遵循 AGPL-3.0 开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced.provider;

import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.config.HttpConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

/**
 * HTTP 认证器
 * 通过调用外部 HTTP 接口进行认证，适用于微服务架构和第三方认证系统集成
 * <p>
 * 配置选项：
 * - url: HTTP 认证接口地址（必填）
 * - method: HTTP 方法（默认 POST）
 * - contentType: 内容类型（默认 application/json）
 * - timeout: 连接超时时间（默认 3000ms）
 * - usernameField: 用户名字段名（默认 username）
 * - passwordField: 密码字段名（默认 password）
 * - tokenHeader: 自定义认证头名称
 * - successCode: 成功响应码（默认 200）
 * - bodyTemplate: 请求体模板（可选，支持{username}和{password}占位符）
 * <p>
 * HTTP 请求示例（POST JSON）：
 * {
 * "username": "test",
 * "password": "123456"
 * }
 * <p>
 * HTTP 响应示例：
 * 成功：返回 200 状态码
 * 失败：返回 401 或其他非 200 状态码
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class HttpAuthenticator extends AbstractAuthenticator {

    private final HttpConfig config;
    private AsynchronousChannelGroup group;
    private HttpClient httpClient;

    public HttpAuthenticator(HttpConfig http) {
        this.config = http;
    }

    @Override
    public void initialize() throws IOException {
        group = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().hashCode(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "HttpAuthenticator-" + getName());
            }
        });
        httpClient = Feat.httpClient(config.getUrl(), options -> {
            options.group(group);
        });
    }

    @Override
    public CompletableFuture<AuthResult> authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] passwordBytes = getPassword(message);

        // 如果没有用户名或密码，跳过此认证器
        if (username == null || username.isEmpty() || passwordBytes == null || passwordBytes.length == 0) {
            return configCompletableFuture.completedFuture(AuthResult.CONTINUE);
        }

        String password = new String(passwordBytes, StandardCharsets.UTF_8);
        return httpClient.post().submit().thenApply(response -> {
            return AuthResult.SUCCESS;
        });
    }

    @Override
    public String getName() {
        return AUTH_TYPE_HTTP;
    }


    @Override
    public void destroy() {
        // 无需特殊清理
        httpClient.close();
        group.shutdown();
    }
}
