/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.controller;

import com.alibaba.fastjson2.JSONObject;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.PluginItem;
import tech.smartboot.mqtt.plugin.openapi.to.PluginMarket;

import java.util.List;

/**
 * @author 三刀
 * @version v1.0 4/21/25
 */
@Controller(async = true, value = OpenApi.BASE_API + "/plugin")
public class PluginManagerController {

    @RequestMapping("/market")
    public RestResult<List<PluginItem>> market() {
        Yaml yaml = new Yaml();
        Object object = yaml.load(PluginManagerController.class.getClassLoader().getResourceAsStream("market.yaml"));
        String configJson = JSONObject.toJSONString(object);
        PluginMarket market = JSONObject.parseObject(configJson, PluginMarket.class);
        return RestResult.ok(market.getPlugins());
    }


    @RequestMapping("/install")
    public RestResult<Void> install(@Param("id") String id) {
        System.out.println("install: " + id);
        return RestResult.ok(null);
    }

    @RequestMapping("/uninstall")
    public RestResult<Void> uninstall(@Param("id") String pluginKey) {
        return RestResult.ok(null);
    }
}
