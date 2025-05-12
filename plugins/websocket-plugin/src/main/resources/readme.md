# WebSocket Plugin

## 功能概述
本插件为MQTT Broker提供WebSocket协议支持，允许客户端通过WebSocket连接进行MQTT通信。

## 技术实现
- 基于Feat框架的HTTP服务器实现WebSocket升级
- 支持MQTT over WebSocket协议（Sec-WebSocket-Protocol: mqtt）
- 使用ByteBuffer处理二进制消息流
- 与核心Broker共享相同的消息处理器和会话管理

## 配置参数
```yaml
port: 8084  # WebSocket监听端口
```

## 使用示例
```javascript
// 使用MQTT.js客户端连接示例
const client = mqtt.connect('ws://localhost:8080/mqtt', {
  protocol: 'ws',
  path: '/mqtt'
})
```

## 技术支持
- 作者：三刀（zhengjunweimail@163.com）
- 供应商：smart-mqtt