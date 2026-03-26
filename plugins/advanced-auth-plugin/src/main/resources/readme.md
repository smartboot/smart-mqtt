# Advanced Auth Plugin for smart-mqtt

## 简介

高级认证插件为 smart-mqtt 提供了企业级的认证能力，支持认证链、多种认证方式和密码编码。

## 特性

- **认证链**：多个认证器按优先级顺序执行
- **多种认证方式**：内存、文件、HTTP、JWT、Redis
- **密码编码**：支持明文、SHA256、Base64
- **匿名访问**：可配置是否允许匿名连接
- **动态配置**：支持运行时更新用户配置

## 快速开始

1. 将插件JAR文件放入 smart-mqtt 的 plugins 目录
2. 复制 advanced-auth-plugin.yaml 到 smart-mqtt 根目录
3. 根据需求修改配置
4. 启动 smart-mqtt

## 配置说明

### 文件认证

文件格式（每行一个用户）：
```
# 这是注释
admin:admin123
user:password123
```

### 内存认证

在配置文件中直接定义用户：
```yaml
authenticators:
  - name: memory
    type: memory
    options:
      users:
        admin: admin123
        user: user123
```

### JWT认证

使用JWT Token进行认证：
```yaml
authenticators:
  - name: jwt
    type: jwt
    options:
      secret: your-secret-key
      issuer: mqtt-server
      usernameClaim: sub
```

MQTT连接时，将JWT Token作为密码传入。

### Redis认证

从Redis查询用户密码：
```yaml
authenticators:
  - name: redis
    type: redis
    options:
      host: localhost
      port: 6379
      password: 
      database: 0
      keyPrefix: mqtt:auth:
```

### HTTP认证

调用外部HTTP接口进行认证：
```yaml
authenticators:
  - name: http
    type: http
    options:
      url: http://localhost:8080/api/auth
      method: POST
      timeout: 5000
      bodyTemplate: '{"username":"{{username}}","password":"{{password}}"}'
      successCondition: response.status == 200
```

## 认证链执行流程

1. 按 order 从小到大依次执行认证器
2. 认证器返回 SUCCESS：认证通过，停止后续认证
3. 认证器返回 FAILURE：认证失败，停止后续认证
4. 认证器返回 CONTINUE：继续下一个认证器

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
