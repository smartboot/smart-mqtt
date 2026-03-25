# Smart-MQTT

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.5.2-green.svg)](https://github.com/smartboot/smart-mqtt/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/smartboot/smart-mqtt.svg)](https://hub.docker.com/r/smartboot/smart-mqtt)

## Introduction

smart-mqtt is an MQTT Broker service designed for enterprise-level IoT scenarios with tens of thousands of device connections. It is the first truly IoT-oriented solution released by the smartboot open-source organization. Developed in Java, its underlying communication is based on the asynchronous non-blocking communication framework [smart-socket](https://github.com/smartboot/smart-socket), implementing the complete MQTT v3.1.1/v5.0 protocol.

![Project Architecture](https://smartboot.tech/smart-mqtt/_astro/framework.Bj8Uk056_1FS6vN.svg)

### Core Advantages

| Advantage | Business Value |
|-----------|----------------|
| **🚀 Ultra-High Performance** | Millions of connections and tens of millions of throughput on a single machine. Support massive devices with ordinary servers. The larger the connection volume, the more obvious the cost advantage. |
| **🔧 Open Architecture** | Plugin-based design for on-demand extension. Southbound adaptation to multi-protocol devices, northbound bridging to enterprise systems. No feature bloat. |
| **☕ Java Ecosystem** | Zero-barrier integration with existing tech stacks. Teams can get started quickly with mature operation and maintenance toolchains. No long-term maintenance burden. |
| **🔄 Standard Protocol** | Full compliance with MQTT 3.1.1/5.0. No vendor lock-in, business autonomy and controllability, smooth migration at any time. |
| **🇨🇳 Autonomous and Controllable** | Fully self-developed core components with transparent and secure code. Complies with government and enterprise information innovation requirements. |

> **Important Notice**: The smart-mqtt code is for personal learning use only. **Any individual or organization is not authorized to use this product for commercial purposes without permission.**

## 🚀 Quick Start

### 📥 Download

- **GitHub Releases**: https://github.com/smartboot/smart-mqtt/releases
- **Gitee Releases**: https://gitee.com/smartboot/smart-mqtt/releases

### Option 1: Docker Quick Start (Recommended)

```bash
docker run --name smart-mqtt \
  -p 1883:1883 \
  -p 18083:18083 \
  -e ENTERPRISE_ENABLE=true \
  -d smartboot/smart-mqtt:latest
```

After the service starts, you can access it through the following ports:
- **MQTT Service Port**: `1883`
- **Management Panel Port**: `18083` (Default credentials: smart-mqtt / smart-mqtt)

<details>
<summary>📋 Docker Compose Deployment (Multi-node Cluster)</summary>

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

### Option 2: Local Installation Package

Download the pre-compiled installation package from the Release page:

```bash
# Download the latest version of the installation package
# Please visit the GitHub Release page to download:
# https://github.com/smartboot/smart-mqtt/releases

# Extract the installation package
tar -xzf smart-mqtt-*.tar.gz
cd smart-mqtt-*

# Start the service
./bin/start.sh
```

## ✨ Product Features

### 🛠️ Core Technology

- **Domestic Origin**: From underlying communication (smart-socket) to application-layer Broker service (smart-mqtt), all are self-developed
- **Extremely Lightweight**: Minimal external dependencies, distribution package under 800KB
- **High Performance, Low Consumption**: Utilizes design and algorithm techniques to fully leverage hardware capabilities, TPS up to 10 million/second

### 🚀 Deployment Experience

- **Out of the Box**: Start MQTT Broker service with zero configuration
- **Flexible Extension**: Provides highly customizable capabilities through plugin mechanism
- **Multi-platform Support**: Supports Docker, local deployment, source code compilation and other deployment methods

### 📊 Protocol Support

- **Complete Protocol**: Implements MQTT v3.1.1 and v5.0 protocols
- **High Concurrency**: Supports million-level device connections
- **QoS Support**: Supports QoS 0, 1, 2 message quality levels

### 🎯 Performance

| Scenario | QoS0 | QoS1 | QoS2 |
|:---------|:----:|:----:|:----:|
| Message Subscription (2000 subscribers, 128 Topics) | 10M/s | 5.4M/s | 3.2M/s |
| Message Publishing (2000 publishers, 128 Topics) | 970K/s | 630K/s | 520K/s |

## 📦 Download

You can get the latest release from the following channels:

- **GitHub Releases**: https://github.com/smartboot/smart-mqtt/releases
- **Docker Hub**: https://hub.docker.com/r/smartboot/smart-mqtt

## 🏗️ Project Structure

```
smart-mqtt/
├── smart-mqtt-broker/        # MQTT Broker main module
├── smart-mqtt-client/        # MQTT Client SDK
├── smart-mqtt-common/        # Common module
├── smart-mqtt-plugin-spec/   # Plugin specification definition
├── smart-mqtt-maven-plugin/  # Maven plugin
├── smart-mqtt-bench/         # Performance testing tool
├── plugins/                  # Plugin collection
│   ├── enterprise-plugin/    # Enterprise plugin (Web management console)
│   ├── cluster-plugin/       # Cluster plugin
│   ├── websocket-plugin/     # WebSocket plugin
│   ├── mqtts-plugin/         # MQTT over SSL/TLS plugin
│   ├── redis-bridge-plugin/  # Redis bridge plugin
│   ├── simple-auth-plugin/   # Simple authentication plugin
│   ├── memory-session-plugin/# Memory session plugin
│   └── bench-plugin/         # Performance benchmarking plugin
├── pages/                    # Documentation website
├── docker-compose.yml        # Docker compose file
└── Makefile                  # Build script
```

## 🔌 Plugin Ecosystem

smart-mqtt adopts a plugin-based architecture design. The enterprise-plugin provides a fully-featured Web management console and powerful plugin lifecycle management capabilities.

### Official Plugins

| Plugin | Description |
|--------|-------------|
| **enterprise-plugin** | Enterprise core plugin, providing Web management console, RESTful API, user management, License management and other features |
| **cluster-plugin** | Cluster plugin, supports multi-node cluster deployment, achieves load balancing and high availability |
| **websocket-plugin** | WebSocket plugin, allows clients to communicate via WebSocket for MQTT |
| **mqtts-plugin** | MQTT over SSL/TLS plugin, provides secure encrypted communication capabilities |
| **redis-bridge-plugin** | Redis bridge plugin, enables message integration with Redis |
| **simple-auth-plugin** | Simple authentication plugin, provides basic username/password authentication |
| **memory-session-plugin** | Memory session plugin, provides memory-based session state management |
| **bench-plugin** | Performance benchmarking plugin, built-in performance testing tool |

### Plugin Management Capabilities

- **Hot Plugging**: Dynamic plugin loading, start/stop without service restart
- **Configuration Management**: Online plugin configuration modification, takes effect in real-time
- **Plugin Marketplace**: Connect to official plugin repository, browse, search and download published plugins
- **Local Upload**: Support uploading custom developed JAR packages for installation

## 📖 Documentation Resources

- **Official Documentation**: [https://smartboot.tech/smart-mqtt/](https://smartboot.tech/smart-mqtt/)
- **Online Demo**: [http://115.190.30.166:8083/](http://115.190.30.166:8083/) (Credentials: smart-mqtt / smart-mqtt)
- **Issue Feedback**: [GitHub Issues](https://github.com/smartboot/smart-mqtt/issues)

## 📝 Release Notes

### smart-mqtt broker v1.5.2 (2026-03-04)

For details, please see: [Changelog](https://smartboot.tech/smart-mqtt/product/changelog/)

## 📜 Project History

- **2018**: Created smart-mqtt project, completed basic protocol codec structure
- **2019-2021**: Project was mostly inactive, focus was on improving the underlying communication framework smart-socket performance
- **2022**: Restarted smart-mqtt, basically completed MQTT Broker and Client functionality development
- **2023**: smart-mqtt enterprise edition initiated
- **2025**: smart-mqtt enterprise edition fully open-sourced

## 🤝 References

1. 《MQTT Protocol 3.1.1 Chinese Version》
2. [moquette](https://github.com/moquette-io/moquette)

---

**Notice**: Commercial use requires authorization! For details, please visit [smartboot official website](https://smartboot.tech/).

**License**: GNU Affero General Public License version 3