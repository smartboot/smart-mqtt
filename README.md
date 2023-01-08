## 项目介绍
smart-mqtt 是用 java 语言开发的 MQTT Broker 服务，也是 smartboot 组织下首款真正意义上面向物联网的解决方案。旨在帮助企业以较低的成本快速搭建稳定、可靠的物联网服务，助力万物互联互通。
![输入图片说明](https://smartboot.tech/assets/img/framework.7f1623ff.png)

smart-mqtt 底层通信采用了异步非阻塞通信框架 smart-socket，现已实现了完整的 mqtt v3.1.1 协议规范，未来还将考虑支持 mqtt v5.0 及其他物联网协议。

## 项目发展
- 2018年，创建 smart-mqtt 项目，完成基本的协议编解码结构搭建。
- 2019~2021年，项目基本处于停更状态，期间重心在于提升底层通信框架 smart-socket 的性能。
- 2022年，重启 smart-mqtt。基本完成 mqtt  broker 和 client 的功能开发。

## 产品特色

- 国产血统：从底层通信（smart-socket）直至应用层 Broker 服务（smart-mqtt）皆为自研。
- 开箱即用：零配置即可启动 MQTT Broker 服务。
- 灵活扩展：通过插件机制，提供高度自由的定制化能力。
- 高能低耗：运用设计和算法技巧充分发挥硬件能力。
- 极致轻量：极少的外部依赖，发行包不足 3MB。


## 推荐阅读
- [加入企业支持计划](https://smartboot.gitee.io/smart-mqtt/)
- [快速上手](https://smartboot.gitee.io/smart-mqtt/quickstart.html)
- [在线服务](https://smartboot.gitee.io/smart-mqtt/service.html)
- [关于企业版](https://smartboot.gitee.io/smart-mqtt/enterprise.html)


## 参考资料
1. 《MQTT协议3.1.1中文版》
2. [moquette](https://github.com/moquette-io/moquette)