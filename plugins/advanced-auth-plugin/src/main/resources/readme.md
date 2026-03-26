# Advanced Auth Plugin for smart-mqtt

## 简介

高级认证插件为 smart-mqtt 提供了企业级的认证能力，支持认证链、多种认证方式和密码编码。

## 特性

- **认证链**：多个认证器按配置顺序执行，可自定义认证链
- **多种认证方式**：HTTP、Redis、MySQL
- **密码编码**：支持明文、SHA256、Base64
- **匿名访问**：可配置是否允许匿名连接
- **动态配置**：支持运行时更新用户配置

## 快速开始

1. 将插件JAR文件放入 smart-mqtt 的 plugins 目录
2. 复制 advanced-auth-plugin.yaml 到 smart-mqtt 根目录
3. 根据需求修改配置
4. 启动 smart-mqtt

## 配置说明

### 认证链配置

可以通过 `authenticationChain` 字段自定义认证器的执行顺序：

```yaml
# 认证链顺序（按此顺序执行认证器）
authenticationChain:
  - redis
  - mysql
  - http
```

如果不配置 `authenticationChain`，则按照 `order` 字段从小到大排序执行。

### Redis 认证

从 Redis 查询用户密码：
```yaml
authenticators:
  - name: redis
    type: redis
    order: 10
    passwordEncoder: sha256
    options:
      host: localhost
      port: 6379
      password: 
      database: 0
      keyPrefix: mqtt:auth:
      connectionTimeout: 2000
```

Redis 中存储格式：`mqtt:auth:{username}` = `{password}`

### MySQL 认证

从 MySQL 数据库查询用户凭证：
```yaml
authenticators:
  - name: mysql
    type: mysql
    order: 20
    passwordEncoder: sha256
    options:
      url: jdbc:mysql://localhost:3306/mqtt?useSSL=false&serverTimezone=UTC
      username: root
      password: your-password
      driverClass: com.mysql.cj.jdbc.Driver
      tableName: mqtt_users
      usernameColumn: username
      passwordColumn: password
      whereClause: " AND status=1"
      connectionTimeout: 3000
      maxConnections: 5
```

数据表结构示例：
```sql
CREATE TABLE mqtt_users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(256) NOT NULL,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### HTTP 认证

调用外部 HTTP 接口进行认证：
```yaml
authenticators:
  - name: http
    type: http
    order: 30
    options:
      url: http://localhost:8080/api/auth
      method: POST
      contentType: application/json
      timeout: 5000
      usernameField: username
      passwordField: password
      successCode: 200
```

HTTP 请求示例（POST JSON）：
```json
{
  "username": "test",
  "password": "123456"
}
```

响应码为 200 表示认证成功，其他状态码表示失败。

## 认证链执行流程

1. 如果配置了 `authenticationChain`，则按配置的顺序依次执行认证器
2. 如果未配置 `authenticationChain`，则按 `order` 字段从小到大排序执行
3. 认证器返回 SUCCESS：认证通过，停止后续认证
4. 认证器返回 FAILURE：认证失败，停止后续认证
5. 认证器返回 CONTINUE：继续下一个认证器
6. 所有认证器都返回 CONTINUE，默认拒绝连接

## 密码编码

支持以下编码方式：
- `plain`：明文（默认）
- `sha256`：SHA-256哈希
- `base64`：Base64编码

在配置文件中指定：
```yaml
authenticators:
  - name: file
    type: file
    passwordEncoder: sha256
```

## 注意事项

1. 生产环境建议使用 HTTPS 和加密存储
2. 定期更新 JWT 密钥和密码
3. 合理配置认证超时时间
4. 监控认证失败日志

## 许可证

AGPL-3.0
