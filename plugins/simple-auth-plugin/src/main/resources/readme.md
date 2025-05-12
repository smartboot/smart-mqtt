`simple auth plugin` 是一个简单的MQTT认证插件，提供基本的用户名密码认证功能。

## 功能特性

- 支持多用户配置
- 简单的用户名/密码认证机制
- 易于配置和使用

## 配置方式

在`plugin.yaml`中配置用户账号信息，格式如下：

```yaml
accounts:
  - username: admin
    password: admin
```

## 使用说明

1. 将插件安装到MQTT服务器
2. 在`plugin.yaml`中配置用户账号
3. 重启MQTT服务使配置生效

## 注意事项

- 密码以明文形式存储，请妥善保管配置文件
- 建议在生产环境中使用更安全的认证方式