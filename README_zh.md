# Smart-MQTT

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-latest-green.svg)](https://gitee.com/smartboot/smart-mqtt/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/smartboot/smart-mqtt.svg)](https://hub.docker.com/r/smartboot/smart-mqtt)

## 项目介绍

smart-mqtt 是专为拥有上万级设备连接量的企业级物联网场景设计的 MQTT Broker 服务，也是 smartboot 开源组织推出的首款真正意义上面向物联网的解决方案。采用 Java 语言开发，底层通信基于异步非阻塞通信框架 [smart-socket](https://gitee.com/smartboot/smart-socket)，实现了完整的 MQTT v3.1.1/v5.0 协议。

![项目架构](https://smartboot.tech/smart-mqtt/_astro/framework.Bj8Uk056_1FS6vN.svg)

### 核心优势

| 优势 | 业务价值 |
|------|----------|
| **🚀 超高性能** | 单机百万连接、千万吞吐，用普通服务器支撑海量设备，连接量越大成本优势越明显 |
| **🔧 开放架构** | 插件化设计按需扩展，南向适配多协议设备，北向桥接企业系统，拒绝功能冗余 |
| **☕ Java 生态** | 与现有技术栈零门槛对接，团队快速上手，运维工具链成熟，长期维护无负担 |
| **🔄 标准协议** | 完全遵循 MQTT 3.1.1/5.0，无厂商锁定，业务自主可控，随时平滑迁移 |
| **🇨🇳 自主可控** | 全栈自研核心组件，代码透明安全，符合政企信创合规要求 |

> **重要提示**: smart-mqtt 代码仅供个人学习使用，**任何个体、组织未经授权不得将此产品用于商业目的**。

## 🚀 快速开始

### 📥 下载安装


| 下载渠道 | 链接 |
|:---------|:-----|
| **Gitee 国内镜像** | https://gitee.com/smartboot/smart-mqtt/releases |
| **GitHub 国际源** | https://github.com/smartboot/smart-mqtt/releases |

> 💡 **快速下载命令**（Linux/Mac）
> ```bash
> curl -LO https://gitee.com/smartboot/smart-mqtt/releases/download/v1.5.3/smart-mqtt-full-v1.5.3.zip
> ```

### 方式一：Docker 快速启动（推荐）

```bash
docker run --name smart-mqtt \
  -p 1883:1883 \
  -p 18083:18083 \
  -e ENTERPRISE_ENABLE=true \
  -d smartboot/smart-mqtt:latest
```

服务启动后，可以通过以下端口访问：
- **MQTT 服务端口**：`1883`
- **管理面板端口**：`18083`（默认账号密码：smart-mqtt / smart-mqtt）

<details>
<summary>📋 Docker Compose 部署（多节点集群）</summary>

```yaml
networks:
  mqtt-network:
    driver: bridge
services:
  mqtt-broker:
    container_name: smart-mqtt
    hostname: mqtt-broker
    image: smartboot/smart-mqtt:latest
    networks:
      mqtt-network: null
    environment:
      ENTERPRISE_ENABLE: true
      BROKER_MAXINFLIGHT: 256
    restart: always
    ports:
      - 18083:18083
      - 1883:1883
```

```bash
docker-compose up -d
```
</details>

### 方式二：本地安装包启动

从 Release 页面下载预编译的安装包：

```bash
# 下载最新版本的安装包
# 请前往 Gitee Release 页面下载：
# https://gitee.com/smartboot/smart-mqtt/releases

# 解压安装包
tar -xzf smart-mqtt-*.tar.gz
cd smart-mqtt-*

# 启动服务
./bin/start.sh
```

## ✨ 产品特色

### 🛠️ 核心技术

- **国产血统**：从底层通信（smart-socket）直至应用层 Broker 服务（smart-mqtt）皆为自研
- **极致轻量**：极少的外部依赖，发行包不足 800KB
- **高能低耗**：运用设计和算法技巧充分发挥硬件能力，TPS 高达 1000万/秒

### 🚀 部署体验

- **开箱即用**：零配置即可启动 MQTT Broker 服务
- **灵活扩展**：通过插件机制，提供高度自由的定制化能力
- **多平台支持**：支持 Docker、本地部署、源码编译等多种部署方式

### 📊 协议支持

- **完整协议**：实现了 MQTT v3.1.1 和 v5.0 协议
- **高并发**：支持百万级设备连接
- **QoS 支持**：支持 QoS 0、1、2 三种消息质量等级

### 🎯 性能表现

| 场景 | QoS0 | QoS1 | QoS2 |
|:-----|:----:|:----:|:----:|
| 消息订阅（2000订阅者，128 Topic） | 1000W/s | 540W/s | 320W/s |
| 消息发布（2000发布者，128 Topic） | 97W/s | 63W/s | 52W/s |

## 📦 下载地址

您可以从以下渠道获取最新的发布版本：

- **GitHub Releases**: https://github.com/smartboot/smart-mqtt/releases
- **Gitee Releases**: https://gitee.com/smartboot/smart-mqtt/releases
- **Docker Hub**: https://hub.docker.com/r/smartboot/smart-mqtt

## 🏗️ 项目结构

```
smart-mqtt/
├── smart-mqtt-broker/        # MQTT Broker 主模块
├── smart-mqtt-client/        # MQTT 客户端 SDK
├── smart-mqtt-common/         # 公共模块
├── smart-mqtt-plugin-spec/    # 插件规范定义
├── smart-mqtt-maven-plugin/   # Maven 插件
├── smart-mqtt-bench/          # 性能测试工具
├── plugins/                   # 插件集合
│   ├── enterprise-plugin/     # 企业版插件（Web管理控制台）
│   ├── cluster-plugin/        # 集群插件
│   ├── websocket-plugin/      # WebSocket 插件
│   ├── mqtts-plugin/          # MQTT over SSL/TLS 插件
│   ├── redis-bridge-plugin/   # Redis 桥接插件
│   ├── simple-auth-plugin/    # 简单认证插件
│   ├── memory-session-plugin/ # 内存会话插件
│   └── bench-plugin/          # 性能压测插件
├── pages/                     # 文档网站
├── docker-compose.yml         # Docker 编排文件
└── Makefile                   # 构建脚本
```

## 🔌 插件生态

smart-mqtt 采用插件化架构设计，通过 enterprise-plugin 提供功能完善的 Web 管理控制台和强大的插件生命周期管理能力。

### 官方插件

| 插件 | 功能描述 |
|------|----------|
| **enterprise-plugin** | 企业版核心插件，提供 Web 管理控制台、RESTful API、用户管理、License 管理等功能 |
| **cluster-plugin** | 集群插件，支持多节点集群部署，实现负载均衡和高可用 |
| **websocket-plugin** | WebSocket 插件，允许客户端通过 WebSocket 进行 MQTT 通信 |
| **mqtts-plugin** | MQTT over SSL/TLS 插件，提供安全加密通信能力 |
| **redis-bridge-plugin** | Redis 桥接插件，实现消息与 Redis 的集成 |
| **simple-auth-plugin** | 简单认证插件，提供基本的用户名密码认证功能 |
| **memory-session-plugin** | 内存会话插件，提供基于内存的会话状态管理 |
| **bench-plugin** | 性能压测插件，内置性能测试工具 |

### 插件管理能力

- **热插拔**：插件动态加载、启停，无需重启服务
- **配置管理**：在线修改插件配置，实时生效
- **插件市场**：连接官方插件仓库，浏览、搜索并下载已发布的插件
- **本地上传**：支持上传自定义开发的 JAR 包进行安装

## 📖 文档资源

- **官方文档**: [https://smartboot.tech/smart-mqtt/](https://smartboot.tech/smart-mqtt/)
- **在线体验**: [http://115.190.30.166:8083/](http://115.190.30.166:8083/)（账号密码：smart-mqtt / smart-mqtt）
- **问题反馈**: [Gitee Issues](https://gitee.com/smartboot/smart-mqtt/issues)

## 📝 发版记录

### smart-mqtt broker v1.5.2（2026-03-04）

详情请查看：[发版记录](https://smartboot.tech/smart-mqtt/product/changelog/)

## 📜 项目发展

- **2018年**：创建 smart-mqtt 项目，完成基本的协议编解码结构搭建
- **2019~2021年**：项目基本处于停更状态，期间重心在于提升底层通信框架 smart-socket 的性能
- **2022年**：重启 smart-mqtt，基本完成 MQTT Broker 和 Client 的功能开发
- **2023年**：smart-mqtt 企业版立项
- **2025年**：smart-mqtt 企业版功能全面开源

## 🤝 参考资料

1. 《MQTT协议3.1.1中文版》
2. [moquette](https://github.com/moquette-io/moquette)

---

**注意**: 商业使用请联系授权！详情请访问 [smartboot 官网](https://smartboot.tech/)。

**License**: GNU Affero General Public License version 3