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
    private BrokerContext brokerContext;
    private List<Authenticator> chains;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        this.brokerContext = brokerContext;

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

        // ========== 全局配置区域 ==========
        Item globalItem = Item.Object("globalConfig", "全局配置").col(12);
        globalItem.addItems(Item.Switch("stopOnError", "认证失败时立即停止").tip("开启后，任一认证器失败则立即拒绝连接；关闭则会尝试所有认证器").col(6), Item.Switch("allowAnonymous", "允许匿名访问").tip("开启后，不提供用户名密码也能连接；生产环境建议关闭").col(6));
        schema.addItem(globalItem);

        // ========== 认证器配置区域 ==========
        Item authArray = Item.ItemArray("authenticators", "认证器配置列表").col(12);

        // 认证器基本配置
        authArray.addItems(Item.String("name", "认证器名称").tip("自定义认证器的唯一标识").col(4), Item.String("type", "认证器类型").tip("http: HTTP 认证，redis: Redis 认证，mysql: MySQL 认证").addEnums(tech.smartboot.mqtt.plugin.spec.schema.Enum.of("http", "HTTP 认证"), tech.smartboot.mqtt.plugin.spec.schema.Enum.of("redis", "Redis 认证"), tech.smartboot.mqtt.plugin.spec.schema.Enum.of("mysql", "MySQL 认证")).col(4), Item.Int("order", "执行顺序").tip("数值越小越优先执行，默认 100").col(4), Item.Switch("enabled", "启用").tip("关闭后将跳过此认证器").col(3), Item.String("passwordEncoder", "密码编码").tip("plain: 明文，sha256: SHA-256 哈希，base64: Base64 编码").addEnums(tech.smartboot.mqtt.plugin.spec.schema.Enum.of("plain", "明文"), tech.smartboot.mqtt.plugin.spec.schema.Enum.of("sha256", "SHA-256"), tech.smartboot.mqtt.plugin.spec.schema.Enum.of("base64", "Base64")).col(4));

        // HTTP 认证器配置
        Item httpOpts = Item.Object("options", "HTTP 认证配置").col(12);
        httpOpts.addItems(Item.String("url", "认证接口 URL").tip("外部认证服务地址，示例：http://localhost:8080/api/auth").col(6), Item.String("method", "请求方法").tip("HTTP 请求方法，默认 POST").addEnums(tech.smartboot.mqtt.plugin.spec.schema.Enum.of("POST", "POST"), tech.smartboot.mqtt.plugin.spec.schema.Enum.of("GET", "GET")).col(3), Item.Int("timeout", "超时时间 (ms)").tip("HTTP 请求超时时间，默认 5000ms").col(3), Item.String("usernameField", "用户名字段名").tip("传递用户名的字段名，默认 username").col(6), Item.String("passwordField", "密码字段名").tip("传递密码的字段名，默认 password").col(6));
        authArray.addItems(httpOpts);

        // Redis 认证器配置
        Item redisOpts = Item.Object("options", "Redis 认证配置").col(12);
        redisOpts.addItems(Item.String("host", "Redis 主机").tip("Redis 服务器地址，默认 localhost").col(3), Item.Int("port", "Redis 端口").tip("Redis 服务器端口，默认 6379").col(3), Item.Int("database", "数据库").tip("Redis 数据库编号，默认 0").col(3), Item.String("password", "Redis 密码").tip("Redis 认证密码").col(3), Item.String("keyPrefix", "Key 前缀").tip("存储用户信息的 Key 前缀，默认 mqtt:auth:").col(6), Item.Int("connectionTimeout", "超时时间 (ms)").tip("Redis 连接超时时间，默认 2000ms").col(3));
        authArray.addItems(redisOpts);

        // MySQL 认证器配置
        Item mysqlOpts = Item.Object("options", "MySQL 认证配置").col(12);
        mysqlOpts.addItems(Item.String("url", "JDBC URL").tip("JDBC 连接 URL，示例：jdbc:mysql://localhost:3306/mqtt").col(6), Item.String("username", "数据库用户名").tip("数据库登录用户名").col(3), Item.String("password", "数据库密码").tip("数据库登录密码").col(3), Item.String("tableName", "表名").tip("用户表名，默认 mqtt_users").col(4), Item.String("usernameColumn", "用户名字段").tip("用户名列，默认 username").col(4), Item.String("passwordColumn", "密码字段").tip("密码列，默认 password").col(4));
        authArray.addItems(mysqlOpts);

        schema.addItem(authArray);

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
