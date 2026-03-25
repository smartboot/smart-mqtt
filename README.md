
<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-AGPL--3.0-blue.svg" alt="License"></a>
  <a href="https://github.com/smartboot/smart-mqtt/releases"><img src="https://img.shields.io/badge/version-v1.5.3-green.svg" alt="Version"></a>
  <a href="https://hub.docker.com/r/smartboot/smart-mqtt"><img src="https://img.shields.io/docker/pulls/smartboot/smart-mqtt.svg" alt="Docker"></a>
  <a href="https://smartboot.tech/smart-mqtt/"><img src="https://img.shields.io/badge/docs-Documentation-blue.svg" alt="Documentation"></a>
</p>

<p align="center">
  <b>High-Performance, Plugin-based Enterprise MQTT Broker</b><br>
  Millions of connections, tens of millions of messages per second
</p>

---

## Introduction

smart-mqtt is a high-performance MQTT Broker designed for enterprise IoT scenarios. Developed in Java with underlying communication based on the self-developed asynchronous non-blocking communication framework [smart-socket](https://github.com/smartboot/smart-socket), fully implementing the MQTT v3.1.1 and v5.0 protocol specifications.

![Project Architecture](https://smartboot.tech/smart-mqtt/_astro/framework.Bj8Uk056_1FS6vN.svg)

### Key Advantages

- **Ultra-High Performance** - Asynchronous non-blocking I/O architecture, millions of concurrent connections on a single node, tens of millions of messages per second with ultra-low latency
- **Ultra Lightweight** - Distribution package size < 800KB, minimal dependencies, extremely low resource footprint
- **Zero Configuration** - Out of the box, deploy and run quickly without complex configuration
- **Plugin Architecture** - Modular design with hot-pluggable extensions, load features on demand without service restart
- **Enterprise High Availability** - Native support for multi-node clustering, automatic load balancing and failover
- **Java Ecosystem** - Zero-barrier integration with existing Java tech stacks, seamless development and operations
- **Standards Compliant** - Full compliance with MQTT 3.1.1/5.0 protocol standards, supporting QoS 0/1/2 all quality levels

> ⚠️ **License Notice**: smart-mqtt is for personal learning use only. **Commercial use is prohibited without authorization**. Please contact us for commercial licensing at [smart-mqtt official website](https://smartboot.tech/smart-mqtt/#enterprise).

---

## Quick Start

### Docker Deployment (Recommended)

```bash
docker run --name smart-mqtt \
  -p 1883:1883 \
  -p 18083:18083 \
  -e ENTERPRISE_ENABLE=true \
  -d smartboot/smart-mqtt:latest
```

- `1883` - MQTT service port
- `18083` - Web management console (default credentials: smart-mqtt / smart-mqtt)

### Local Installation

```bash
# Download and extract
curl -LO https://github.com/smartboot/smart-mqtt/releases/download/v1.5.3/smart-mqtt-full-v1.5.3.zip
unzip smart-mqtt-full-v1.5.3.zip && cd smart-mqtt-full-v1.5.3

# Start service
./bin/start.sh
```

---


## Plugin Ecosystem

smart-mqtt adopts a plugin-based architecture. The `enterprise-plugin` provides an enterprise-grade Web management console.

| Plugin | Function | Recommended For |
|--------|----------|-----------------|
| **enterprise-plugin** | Web console, RESTful API, user management | Production environments |
| **cluster-plugin** | Multi-node clustering, load balancing, node discovery | High availability deployments |
| **websocket-plugin** | WebSocket protocol support | Web applications |
| **mqtts-plugin** | SSL/TLS encrypted communication | Security-sensitive scenarios |
| **redis-bridge-plugin** | Message bridging to Redis | Cache integration |
| **simple-auth-plugin** | Username/password authentication, ACL | Basic authentication |

---

## Project History

```mermaid
timeline
    title smart-mqtt Development Timeline
    2018 : Project Founded
         : Completed MQTT protocol codec framework
    2019-2021 : Focused on smart-socket performance optimization
    2022 : Restarted smart-mqtt
         : Completed core Broker and Client functionality
    2023 : Enterprise edition initiated
         : Web console and plugin system development
    2025 : Enterprise features fully open-sourced
         : v1.5.x officially released
    2026 : AI-Powered Management System Rebuild
         : Integrated Feat Agent for intelligent automation
```

---

## Documentation

- 📚 [Official Documentation](https://smartboot.tech/smart-mqtt/) - Complete usage documentation and API reference
- 🖥️ [Live Demo](http://115.190.30.166:8083/) - Credentials: smart-mqtt / smart-mqtt
- 🐛 [Issue Tracking](https://github.com/smartboot/smart-mqtt/issues) - GitHub Issues

---

<p align="center">
  License: <b>AGPL-3.0</b> | 
  <a href="https://smartboot.tech/">smartboot Official Website</a> |
  <a href="README_zh.md">🇨🇳 简体中文</a>
</p>
