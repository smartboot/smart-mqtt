# Smart-MQTT

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.5.1-green.svg)](https://github.com/smartboot/smart-mqtt/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/smartboot/smart-mqtt.svg)](https://hub.docker.com/r/smartboot/smart-mqtt)

## 项目介绍

smart-mqtt 是用 Java 语言开发的 MQTT Broker 服务，也是 smartboot 组织下首款真正意义上面向物联网的解决方案。旨在帮助企业以较低的成本快速搭建稳定、可靠的物联网服务，助力万物互联互通。

![项目架构](https://smartboot.tech/smart-mqtt/_astro/framework.Bj8Uk056_1FS6vN.svg)

smart-mqtt 底层通信采用了异步非阻塞通信框架 smart-socket，现已实现了完整的 MQTT v3.1.1/v5.0 协议。

> **重要提示**: smart-mqtt 代码仅供个人学习使用，**任何个体、组织未经授权不得将此产品用于商业目的**。
> - 在线体验：[http://115.190.30.166:8083/](http://115.190.30.166:8083/)
> - 账号密码：smart-mqtt / smart-mqtt

## 🚀 快速开始

### 方式一：Docker 快速启动

```bash
docker run --name smart-mqtt \
  -p 1883:1883 \
  -p 18083:18083 \
  -d smartboot/smart-mqtt:1.5.1
```

服务启动后，可以通过以下端口访问：
- MQTT 服务端口：1883
- 管理面板端口：18083

### 方式二：本地安装包启动

从 Release 页面下载预编译的安装包：

```bash
# 下载最新版本的安装包
# 请前往 GitHub Release 页面下载：
# https://github.com/smartboot/smart-mqtt/releases

# 解压安装包
tar -xzf smart-mqtt-*.tar.gz
cd smart-mqtt-*

# 启动服务
./bin/start.sh
```


## 📦 下载地址

您可以从以下渠道获取最新的发布版本：

- **GitHub Releases**: https://github.com/smartboot/smart-mqtt/releases
- **Gitee Releases**: https://gitee.com/smartboot/smart-mqtt/releases
- **Docker Hub**: https://hub.docker.com/r/smartboot/smart-mqtt

## 🏗️ 项目结构

```
smart-mqtt/
├── smart-mqtt-broker/       # MQTT Broker 主模块
├── smart-mqtt-client/       # MQTT 客户端 SDK
├── smart-mqtt-common/       # 公共模块
├── smart-mqtt-plugin-spec/  # 插件规范定义
├── smart-mqtt-maven-plugin/ # Maven 插件
├── smart-mqtt-bench/        # 性能测试工具
├── plugins/                 # 插件集合
│   ├── cluster-plugin/      # 集群插件
│   ├── enterprise-plugin/   # 企业版插件
│   └── simple-auth-plugin/  # 简单认证插件
├── docker-compose.yml       # Docker 编排文件
├── Makefile                 # 构建脚本
└── install.sh              # 安装脚本
```

## ✨ 产品特色

### 🛠️ 核心技术

- **国产血统**：从底层通信（smart-socket）直至应用层 Broker 服务（smart-mqtt）皆为自研
- **极致轻量**：极少的外部依赖，发行包不足 800KB
- **高能低耗**：运用设计和算法技巧充分发挥硬件能力

### 🚀 部署体验

- **开箱即用**：零配置即可启动 MQTT Broker 服务
- **灵活扩展**：通过插件机制，提供高度自由的定制化能力
- **多平台支持**：支持 Docker、本地部署、源码编译等多种部署方式

### 📊 协议支持

- **完整协议**：实现了 MQTT v3.1.1 和 v5.0 协议
- **高并发**：支持百万级设备连接
- **QoS 支持**：支持 QoS 0、1、2 三种消息质量等级

---

**注意**: 商业使用请联系授权！
## 项目发展
- 2018年，创建 smart-mqtt 项目，完成基本的协议编解码结构搭建。
- 2019~2021年，项目基本处于停更状态，期间重心在于提升底层通信框架 smart-socket 的性能。
- 2022年，重启 smart-mqtt。基本完成 mqtt  broker 和 client 的功能开发。
- 2023年，smart-mqtt企业版立项。
- 2025年，smart-mqtt企业版功能全面开源。


## 参考资料
1. 《MQTT协议3.1.1中文版》
2. [moquette](https://github.com/moquette-io/moquette)