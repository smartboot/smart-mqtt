# Advanced Auth Plugin for smart-mqtt

## 简介

高级认证插件为 smart-mqtt 提供了企业级的认证能力，支持认证链、多种认证方式和密码编码。

## 特性

- **认证链**：多个认证器按配置顺序执行，可自定义认证链
- **多种认证方式**：HTTP、Redis
- **密码编码**：支持明文、SHA256、Base64
- **匿名访问**：可配置是否允许匿名连接

## 快速开始

1. 将插件 JAR 文件放入 smart-mqtt 的 plugins 目录
2. 复制 plugin.yaml 到 smart-mqtt 根目录
3. 根据需求修改配置
4. 启动 smart-mqtt

## 配置说明

### 基础配置

```yaml
# stopOnError: true - 认证失败时立即拒绝连接
# allowAnonymous: false - 是否允许匿名连接
stopOnError: true
allowAnonymous: false

# 认证链顺序（按此顺序执行认证器）
chain:
  - redis
  - http
```

### 认证链执行流程

1. 按 `chain` 字段配置的顺序依次执行认证器
2. 认证器返回 SUCCESS：认证通过，停止后续认证
3. 认证器返回 FAILURE：认证失败，停止后续认证
4. 认证器返回 CONTINUE：继续下一个认证器
5. 所有认证器都返回 CONTINUE， 默认拒绝连接

### Redis 认证

从 Redis 查询用户凭证进行认证，适用于分布式、高并发场景：

```yaml
redis:
  # Redis 地址 (redis://host:port 格式)
  address: redis://localhost:6379
  # Redis 用户名 (可选)
  username: 
  # Redis 密码 (可选)
  password: 
  # 数据库索引 (默认 0)
  database: 0
  # 连接超时时间，单位毫秒 (默认 20000)
  connectionTimeout: 20000
```

Redis Hash 存储格式：
- Key: `smart-mqtt:auth:{username}`
- 字段:
  - `password_hash`: 密码哈希值 (必填)
  - `salt`: 盐值 (可选，有盐值时密码 = salt + 原始密码)
  - `password_encoder`: 密码编码器名称 (可选，默认 sha256)

示例：
```bash
HSET smart-mqtt:auth:user1 password_hash "e3b0c44..." salt "salt123" password_encoder "sha256"
```

### HTTP 认证

调用外部 HTTP 接口进行认证，POST JSON 格式，适用于微服务架构和第三方认证系统集成：

```yaml
http:
  # 认证接口 URL (必填)
  url: http://localhost:80/80/api/auth
  # 请求超时时间，单位毫秒 (默认 5000)
  timeout: 5000
  # 自定义请求头 (可选)
  headers: 
    # Authorization: Bearer token
```

HTTP 请求说明：
- 方法: POST
- Content-Type: application/json
- 请求体 JSON：
```json
{
  "username": "test",
  "password": "123456",
  "clientId": "client-001"
}
```

响应码为 200 表示认证成功，其他状态码表示认证失败。

## 密码编码

支持以下编码方式（通过 Redis 中的 `password_encoder` 字段指定）：
- `plain`：明文
- `sha256`：SHA-256 哈希
- `base64`：Base64 编码

## 注意事项

1. 生产环境建议使用 HTTPS 和加密存储
2. 定期更新密码
3. 合理配置认证超时时间
4. 监控认证失败日志

## 许可证

AGPL-3.0
