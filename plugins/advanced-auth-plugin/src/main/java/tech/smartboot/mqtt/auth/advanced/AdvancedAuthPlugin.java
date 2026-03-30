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

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.mqtt.auth.advanced.config.PluginConfig;
import tech.smartboot.mqtt.auth.advanced.provider.HttpAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.RedisAuthenticator;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.schema.Item;
import tech.smartboot.mqtt.plugin.spec.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 高级认证插件
 * <p>
 * 特性：
 * 1. 支持认证链：多个认证器按优先级顺序执行
 * 2. 内置多种认证方式：HTTP、Redis、MySQL
 * 3. 支持密码编码：明文、SHA256、Base64
 * 4. 支持匿名访问
 *
 * @author 三刀
 * @version v1.0 2026/3/25
 */
public class AdvancedAuthPlugin extends Plugin {

    private PluginConfig config;
    private List<Authenticator> chains;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {

        log("==============================================");
        log("正在初始化高级认证插件...");
        log("==============================================");

        // 加载配置
        config = loadPluginConfig(PluginConfig.class);

        chains = new ArrayList<>(config.getChain().size());
        for (String authName : config.getChain()) {
            if (Authenticator.AUTH_TYPE_HTTP.equals(authName)) {
                chains.add(new HttpAuthenticator(config.getHttp()));
            } else if (Authenticator.AUTH_TYPE_REDIS.equals(authName)) {
                chains.add(new RedisAuthenticator(config.getRedis()));
            } else {
                log("未知的认证器：" + authName);
            }
        }

        if (FeatUtils.isEmpty(chains)) {
            log("没有启用的认证器，请检查配置");
            throw new IllegalStateException("没有启用的认证器");
        }

        for (Authenticator chain : chains) {
            chain.initialize();
        }

        // 订阅CONNECT事件
        subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();

            // 如果已经认证失败或断开，直接返回
            if (session.isDisconnect()) {
                object.getFuture().complete(null);
                return;
            }

            MqttConnectMessage message = object.getObject();

            // 检查是否匿名访问
            if (isAnonymous(message)) {
                if (config.isAllowAnonymous()) {
                    session.setAuthorized(true);
                    log("匿名访问已授权: " + session.getClientId());
                } else {
                    log("匿名访问被拒绝: " + session.getClientId());
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                }
                object.getFuture().complete(null);
                return;
            }


            // 使用 thenCompose 实现异步链式认证
            CompletableFuture<AuthResult> chainFuture = CompletableFuture.completedFuture(AuthResult.CONTINUE);
            for (Authenticator auth : chains) {
                chainFuture = chainFuture.thenCompose(result -> {
                    if (result != AuthResult.CONTINUE) {
                        return CompletableFuture.completedFuture(result);
                    }
                    try {
                        return auth.authenticate(session, message)
                                .exceptionally(e -> {
                                    log("认证器异常: " + auth.getName() + ", error=" + e.getMessage());
                                    return config.isStopOnError() ? AuthResult.FAILURE : AuthResult.CONTINUE;
                                });
                    } catch (Exception e) {
                        log("认证器异常: " + auth.getName() + ", error=" + e.getMessage());
                        return CompletableFuture.completedFuture(
                                config.isStopOnError() ? AuthResult.FAILURE : AuthResult.CONTINUE);
                    }
                });
            }

            // 处理最终结果（CONTINUE 视为失败）
            chainFuture.thenAccept(result -> {
                if (result == AuthResult.SUCCESS) {
                    session.setAuthorized(true);
                    log("认证成功: clientId=" + session.getClientId());
                } else {
                    log("认证失败: clientId=" + session.getClientId());
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, session);
                }
                object.getFuture().complete(null);
            });
        });

        log("==============================================");
        log("高级认证插件初始化完成");
        log("认证器数量: " + chains.size());
        log("==============================================");
    }

    @Override
    protected void destroyPlugin() {
        log("正在关闭高级认证插件...");

        if (chains != null) {
            chains.forEach(Authenticator::destroy);
        }

        log("高级认证插件已关闭");
    }

    @Override
    public String getVersion() {
        return Options.VERSION;
    }

    @Override
    public String getVendor() {
        return Options.VENDOR;
    }

    @Override
    public String pluginName() {
        return "advanced-auth-plugin";
    }

    @Override
    public Schema schema() {
        Schema schema = new Schema();

        // ========== 全局配置 ==========
        schema.addItem(
                Item.Switch("stopOnError", "认证失败时立即停止")
                        .tip("开启后，任一认证器失败则立即拒绝连接；关闭则会尝试所有认证器")
                        .col(6)
        );
        schema.addItem(
                Item.Switch("allowAnonymous", "允许匿名访问")
                        .tip("开启后，不提供用户名密码也能连接；生产环境建议关闭")
                        .col(6)
        );
        // ========== 认证链配置区域 ==========
        Item chainArray = Item.MultiEnum("chain", "认证链顺序")
                .addEnums(
                        tech.smartboot.mqtt.plugin.spec.schema.Enum.of("redis", "Redis 认证"),
                        tech.smartboot.mqtt.plugin.spec.schema.Enum.of("http", "HTTP 认证")
//                        tech.smartboot.mqtt.plugin.spec.schema.Enum.of("mysql", "MySQL 认证")
                )
                .col(12)
                .tip("按顺序执行认证器，支持 redis、http、mysql，未配置的认证器将被跳过");
        schema.addItem(chainArray);
        // ========== HTTP 认证器配置区域 ==========
        Item httpItem = Item.Object("http", "HTTP 认证器配置").col(12);
        httpItem.addItems(
                Item.String("url", "认证接口 URL")
                        .tip("外部认证服务地址，示例：http://localhost:8080/api/auth")
                        .col(8),
                Item.Int("timeout", "超时时间 (ms)")
                        .tip("HTTP 请求超时时间，默认 5000ms")
                        .col(4)
        );
        // HTTP Header 配置
        Item headersArray = Item.ItemArray("headers", "自定义请求头列表").col(12)
                .tip("添加自定义 HTTP 请求头，例如 Authorization、Content-Type 等");
        headersArray.addItems(
                Item.String("name", "请求头名称")
                        .tip("请求头的字段名，例如 Authorization、X-API-Key")
                        .col(6),
                Item.String("value", "请求头值")
                        .tip("请求头的字段值")
                        .col(6)
        );
        httpItem.addItems(headersArray);
        schema.addItem(httpItem);

        // ========== Redis 认证器配置区域 ==========
        Item redisItem = Item.Object("redis", "Redis 认证器配置").col(12);
        redisItem.addItems(
                Item.String("address", "Redis 地址")
                        .tip("Redis 服务器地址，格式：redis://host:port，示例：redis://localhost:6379")
                        .col(6),
                Item.String("username", "Redis 用户名")
                        .tip("Redis 认证用户名（可选）")
                        .col(3),
                Item.String("password", "Redis 密码")
                        .tip("Redis 认证密码")
                        .col(3),
                Item.Int("database", "数据库编号")
                        .tip("Redis 数据库编号，默认 0")
                        .col(3),
                Item.Int("connectionTimeout", "连接超时 (ms)")
                        .tip("Redis 连接超时时间，默认 2000ms")
                        .col(3)
        );
        schema.addItem(redisItem);


        return schema;
    }

    /**
     * 检查是否为匿名连接
     */
    private boolean isAnonymous(MqttConnectMessage message) {
        String username = message.getPayload().userName();
        byte[] password = message.getPayload().passwordInBytes();
        return username == null || username.isEmpty() || password == null || password.length == 0;
    }

}
