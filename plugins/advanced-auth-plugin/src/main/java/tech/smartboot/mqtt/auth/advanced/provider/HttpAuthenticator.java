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

import tech.smartboot.mqtt.auth.advanced.AuthResult;
import tech.smartboot.mqtt.auth.advanced.PluginConfig;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    private String url;
    private String method = "POST";
    private String contentType = "application/json";
    private int timeout = 3000;
    private String usernameField = "username";
    private String passwordField = "password";
    private String tokenHeader;
    private int successCode = 200;
    private String bodyTemplate;
    private PluginConfig.HttpConfig config;

    public HttpAuthenticator(PluginConfig.HttpConfig http) {
        this.config = http;
    }


    @Override
    public AuthResult authenticate(MqttSession session, MqttConnectMessage message) {
        String username = getUsername(message);
        byte[] passwordBytes = getPassword(message);

        // 如果没有用户名或密码，跳过此认证器
        if (username == null || username.isEmpty() || passwordBytes == null || passwordBytes.length == 0) {
            return AuthResult.CONTINUE;
        }

        String password = new String(passwordBytes, StandardCharsets.UTF_8);

        try {
            boolean authenticated = callHttpApi(username, password);

            if (authenticated) {
                return AuthResult.SUCCESS;
            } else {
                return AuthResult.FAILURE;
            }

        } catch (Exception e) {
            // HTTP 调用异常，返回 CONTINUE 让下一个认证器处理
            return AuthResult.CONTINUE;
        }
    }

    @Override
    public String getName() {
        return AUTH_TYPE_HTTP;
    }

    /**
     * 调用 HTTP 认证接口
     */
    private boolean callHttpApi(String username, String password) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        try {
            // 设置基本连接参数
            conn.setRequestMethod(method);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);

            // 设置请求头
            if (contentType != null && !contentType.isEmpty()) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Connection", "close");

            // 添加自定义认证头（如果有）
            if (tokenHeader != null && !tokenHeader.isEmpty()) {
                conn.setRequestProperty("Authorization", tokenHeader);
            }

            // 准备请求体
            byte[] body = prepareBody(username, password);
            if (body != null && body.length > 0) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", String.valueOf(body.length));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                    os.flush();
                }
            }

            // 获取响应码
            int responseCode = conn.getResponseCode();

            return responseCode == successCode;

        } finally {
            conn.disconnect();
        }
    }

    /**
     * 准备请求体
     */
    private byte[] prepareBody(String username, String password) {
        if (bodyTemplate != null && !bodyTemplate.isEmpty()) {
            // 使用自定义模板
            String body = bodyTemplate
                    .replace("{username}", escapeJson(username))
                    .replace("{password}", escapeJson(password));
            return body.getBytes(StandardCharsets.UTF_8);
        } else {
            // 默认 JSON 格式
            String json = String.format(
                    "{\"%s\":\"%s\",\"%s\":\"%s\"}",
                    escapeJson(usernameField),
                    escapeJson(username),
                    escapeJson(passwordField),
                    escapeJson(password)
            );
            return json.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public void destroy() {
        // 无需特殊清理
    }
}
