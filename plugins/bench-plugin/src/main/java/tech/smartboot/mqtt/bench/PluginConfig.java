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

/**
 * 压测插件配置
 * 支持 publish 和 subscribe 两种独立的配置
 *
 * @author 三刀
 * @version v1.5.1
 */
public class PluginConfig {

    /**
     * 压测场景: publish-发布压测, subscribe-订阅压测
     */
    private String scenario = "publish";

    /**
     * 发布压测配置
     */
    private PublishConfig publish = new PublishConfig();

    /**
     * 订阅压测配置
     */
    private SubscribeConfig subscribe = new SubscribeConfig();

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public PublishConfig getPublish() {
        return publish;
    }

    public void setPublish(PublishConfig publish) {
        this.publish = publish;
    }

    public SubscribeConfig getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(SubscribeConfig subscribe) {
        this.subscribe = subscribe;
    }
}