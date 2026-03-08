/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.bench;

import java.util.ArrayList;
import java.util.List;

/**
 * 压测插件配置
 * 支持配置多个压测场景，每次选择其中一个运行
 *
 * @author 三刀
 * @version v1.5.2
 */
public class PluginConfig {

    // ==================== 公共参数 ====================
    /**
     * MQTT服务器主机
     */
    private String host = "127.0.0.1";

    /**
     * MQTT服务器端口
     */
    private int port = 1883;

    /**
     * 主题数量
     */
    private int topicCount = 128;

    /**
     * 消息 payload 大小（字节）
     */
    private int payloadSize = 1024;
    // ==================================================

    /**
     * 激活的场景名称
     */
    private String active = "default";

    /**
     * 场景配置列表
     */
    private List<ScenarioConfig> scenarios = new ArrayList<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public List<ScenarioConfig> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioConfig> scenarios) {
        this.scenarios = scenarios;
    }

}