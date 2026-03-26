该插件为 smart-mqtt broker 提供 TLS/SSL 安全传输支持，在传输层对网络连接进行加密，提升通信数据的安全性、完整性和机密性。

## 功能特性

- ✅ 基于 PEM 格式证书的 TLS/SSL 加密通信
- ✅ 支持自定义监听端口和主机地址
- ✅ 支持单向认证（服务器认证）
- ✅ 高性能异步 IO，低延迟


## 配置参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| port | int | 是 | SSL/TLS 监听端口，推荐使用 8883 |
| host | string | 否 | 监听主机地址，默认 0.0.0.0 |
| pem | string | 是 | PEM 格式的证书内容，包含证书和私钥 |

## 使用示例

### 基础配置

```yaml
host: 0.0.0.0
port: 8883
pem: |
  -----BEGIN CERTIFICATE-----
  MIIEsTCCAxmgAwIBAgIQb1DqeyVD0+UBTKynNf3oJzANBgkqhkiG9w0BAQsFADCB
  ...（服务器证书）...
  -----END CERTIFICATE-----
  -----BEGIN PRIVATE KEY-----
  MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC1/iKnsFYqfqtV
  ...（服务器私钥）...
  -----END PRIVATE KEY-----
```

### 生产环境建议

#### 方案一：直接在 Broker 上启用 TLS（适合中小规模部署）

**优势：**
- 简单易用，无需额外组件
- 配置简单，维护成本低

**注意：**
- 会增加 Broker 的 CPU 和内存消耗
- 大规模连接场景下需评估资源消耗

#### 方案二：通过代理或负载均衡终结 TLS（推荐用于大规模部署）

**优势：**
- 不影响 Broker 性能，专注于 MQTT 协议处理
- 提供负载均衡能力，支持水平扩展
- TLS 加解密由专业组件处理，效率更高

**部署架构：**

```mermaid
graph LR
    A[MQTT Client<br/>TLS 加密] -->|端口 8883| B[HAProxy/Nginx<br/>TLS 终结<br/>负载均衡]
    B -->|端口 1883<br/>明文传输 | C1[Smart-MQTT Broker 1<br/>192.168.1.10]
    B -->|端口 1883<br/>明文传输 | C2[Smart-MQTT Broker 2<br/>192.168.1.11]
    
    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C1 fill:#f0f0f0
    style C2 fill:#f0f0f0
```

**HAProxy 配置示例：**

```haproxy
frontend mqtts_frontend
    bind *:8883 ssl crt /etc/haproxy/certs/server.pem
    mode tcp
    default_backend mqtt_broker

backend mqtt_broker
    mode tcp
    balance roundrobin
    server broker1 192.168.1.10:1883 check
    server broker2 192.168.1.11:1883 check
```

**Nginx 配置示例：**

```nginx
stream {
    upstream mqtt_broker {
        server 192.168.1.10:1883;
        server 192.168.1.11:1883;
    }

    server {
        listen 8883 ssl;
        proxy_pass mqtt_broker;
        
        ssl_certificate /etc/nginx/ssl/server.crt;
        ssl_certificate_key /etc/nginx/ssl/server.key;
        ssl_protocols TLSv1.2 TLSv1.3;
    }
}
```

**适用场景：**
- 高并发、大连接数场景（10 万 + 连接）
- 需要集群部署和负载均衡
- 云原生环境（可使用云厂商的 LB 服务）




## 安全最佳实践

1. **使用强加密套件**：仅启用 TLSv1.2 和 TLSv1.3
2. **定期更新证书**：避免证书过期导致服务中断
3. **保护私钥**：设置合适的文件权限（chmod 600）
4. **监控证书有效期**：提前 30 天告警
5. **考虑双向认证**：高安全场景启用客户端证书验证
6. **使用负载均衡**：大规模部署时采用方案二

## 常见问题

### Q: 连接时报 certificate verify failed？
A: 客户端未正确配置 CA 证书，请确保客户端信任服务器证书的颁发机构。

### Q: 性能下降明显？
A: 建议使用负载均衡器终结 TLS，或使用硬件加速卡。

### Q: 如何查看当前 TLS 连接信息？
A: 使用 `openssl s_client -connect host:8883 -state` 诊断连接。

## 技术支持

- 作者：三刀（zhengjunweimail@163.com）
- 供应商：smart-mqtt