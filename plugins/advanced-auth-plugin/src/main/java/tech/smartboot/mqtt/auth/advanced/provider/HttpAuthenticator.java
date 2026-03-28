/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经 smartboot 组织特别许可，AGPL-3.0
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.auth.advanced.provider;

import com.alibaba.fastjson2.JSONObject;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.config.HttpConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP 认证器
 * 通过调用外部 HTTP 接口进行认证，适用于微服务架构和第三方认证系统集成
 */
public class HttpAuthenticator extends AbstractAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(HttpAuthenticator.class.getName());
    private final HttpConfig config;
    private AsynchronousChannelGroup group;
    private HttpClient httpClient;

    public HttpAuthenticator(HttpConfig http) {
        this.config = http;
    }

    @Override
    public void initialize() throws IOException {
        group = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "HttpAuthenticator-" + getName()));
        httpClient = Feat.httpClient(config.getUrl(), options -> {
            options.group(group);
            options.connectTimeout(config.getTimeout());
        });
    }

    @Override
    public CompletableFuture<AuthResult> authenticate(MqttSession session, MqttConnectMessage message) {
        JSONObject body = new JSONObject();
        body.put("username", message.getPayload().userName());
        body.put("password", new String(getPassword(message), StandardCharsets.UTF_8));
        body.put("clientId", session.getClientId());

        byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        // POST 请求
        return httpClient.post().header(header -> {
            // 设置自定义请求头
            if (FeatUtils.isNotEmpty(config.getHeaders())) {
                config.getHeaders().forEach(header::add);
            }
            header.set(HeaderName.CONTENT_TYPE, HeaderValue.ContentType.APPLICATION_JSON);
            header.set(HeaderName.CONTENT_LENGTH, bytes.length);
        }).postBody(postBody -> postBody.write(bytes)).submit().thenApply(response -> {
            if (response.statusCode() == HttpStatus.OK.value()) {
                return AuthResult.SUCCESS;
            } else {
                return AuthResult.FAILURE;
            }
        }).exceptionally(throwable -> {
            log.error("http request exception", throwable);
            return AuthResult.CONTINUE;
        });
    }

    @Override
    public String getName() {
        return AUTH_TYPE_HTTP;
    }

    @Override
    public void destroy() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (group != null) {
            group.shutdown();
        }
    }
}