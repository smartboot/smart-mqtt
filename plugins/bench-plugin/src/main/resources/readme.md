`bench-plugin` 是一个MQTT压测插件，用于对MQTT Broker进行性能压测。

## 功能特性

- 支持两种压测场景：Publish（发布压测）和 Subscribe（订阅压测）
- 公共参数统一配置，减少重复
- 可配置连接数、主题数、消息大小、QoS等参数
- 支持高并发连接
- 实时显示压测状态

## 配置方式

在 `plugin.yaml` 中配置压测参数：

```yaml
# 压测场景: publish-发布压测, subscribe-订阅压测
scenario: publish

# ==================== 公共压测配置 ===================
# MQTT服务器地址
host: 127.0.0.1
# MQTT服务器端口
port: 1883
# 消息负载大小（字节）
payloadSize: 1024
# 主题数量
topicCount: 128
# QoS等级: 0-AtMostOnce, 1-AtLeastOnce, 2-ExactlyOnce
qos: 0

# ==================== 发布压测配置 ===================
publish:
  # 发布者数量
  connections: 1000
  # 每次发布的消息数量
  publishCount: 1
  # 发布间隔（毫秒）
  period: 1

# ==================== 订阅压测配置 ===================
subscribe:
  # 订阅者数量
  connections: 1000
  # 发布者数量（0表示不启动发布者）
  publisherCount: 1
  # 每次发布的消息数量
  publishCount: 1
  # 发布间隔（毫秒）
  publishPeriod: 1
```

## 使用说明

### 发布压测（Publish）

配置 `scenario: publish`，然后在 `publish` 下配置参数。插件将创建多个MQTT客户端连接并持续向Broker发布消息。

### 订阅压测（Subscribe）

配置 `scenario: subscribe`，然后在 `subscribe` 下配置参数。插件将创建多个MQTT客户端连接并订阅主题，同时可以启动发布者向这些主题发送消息以测试消息分发性能。

## 公共参数说明（PluginConfig）

| 参数 | 说明 | 默认值 |
|------|------|--------|
| host | MQTT服务器地址 | 127.0.0.1 |
| port | MQTT服务器端口 | 1883 |
| payloadSize | 消息负载大小（字节） | 1024 |
| topicCount | 主题数量 | 128 |
| qos | QoS等级：0/1/2 | 0 |

## PublishConfig 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| connections | 发布者数量 | 1000 |
| publishCount | 每次发布的消息数量 | 1 |
| period | 发布间隔（毫秒） | 1 |

## SubscribeConfig 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| connections | 订阅者数量 | 1000 |
| publisherCount | 发布者数量（0表示不启动） | 1 |
| publishCount | 每次发布的消息数量 | 1 |
| publishPeriod | 发布间隔（毫秒） | 1 |

## 使用示例

1. 将插件安装到MQTT服务器
2. 根据需要修改 `plugin.yaml` 中的配置参数
3. 重启MQTT服务使配置生效
4. 查看控制台输出的压测状态信息

## 注意事项

- 压测会对服务器产生较大负载，请确保测试环境与生产环境隔离
- 建议根据服务器硬件配置适当调整连接数和主题数
- 在订阅压测场景中，设置 `publisherCount: 0` 可以只测试订阅性能而不发送消息