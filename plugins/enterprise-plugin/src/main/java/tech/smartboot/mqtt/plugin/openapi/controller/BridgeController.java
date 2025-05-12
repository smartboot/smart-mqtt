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
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.convert.BridgeConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.PluginConfigMapper;
import tech.smartboot.mqtt.plugin.dao.model.PluginConfigDO;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.BridgeConfigTO;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.bus.DisposableEventBusSubscriber;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class BridgeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeController.class);

    @Autowired
    private BrokerContext brokerContext;

    private HttpClient client;

    @Autowired
    private PluginConfig pluginConfig;

    @Autowired
    private PluginConfigMapper pluginConfigMapper;

    @PostConstruct
    public void init() {
        client = new HttpClient(pluginConfig.getHttpConfig().getHost(), pluginConfig.getHttpConfig().getPort());
        client.options().connectTimeout(3000);
//        client.configuration().debug(true);
        brokerContext.getEventBus().subscribe(EventType.BROKER_STARTED, new DisposableEventBusSubscriber<BrokerContext>() {
            @Override
            public void consumer(EventType<BrokerContext> eventType, BrokerContext object) {
                List<PluginConfigDO> list = pluginConfigMapper.selectAll();
                list.stream().filter(configDO -> configDO.getStatus() == 1).forEach(configDO -> {
                    try {
                        start(configDO.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/add")
    public RestResult<Void> addBridge(@Param("type") String type) throws IOException {
        PluginConfigDO configDO = new PluginConfigDO();
        configDO.setPluginType(type);
        configDO.setConfig("{}");
        pluginConfigMapper.insert(configDO);
        return RestResult.ok(null);
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/remove")
    public RestResult<Void> deleteBridge(@Param("id") int id) throws Exception {
        //尝试停止
        stop(id);
        int count = pluginConfigMapper.deleteById(id);
        if (count == 1) {
            return RestResult.ok(null);
        } else {
            return RestResult.fail("删除失败");
        }
    }


    @RequestMapping(OpenApi.BASE_API + "/bridge/update")
    public RestResult<Void> start(BridgeConfigTO config) throws IOException {
        PluginConfigDO configDO = new PluginConfigDO();
        configDO.setId(config.getId());
        configDO.setConfig(config.getConfig().toString());
        int count = pluginConfigMapper.updateById(configDO);
        if (count == 1) {
            return RestResult.ok(null);
        }
        System.out.println(JSONObject.toJSONString(config));
        return RestResult.fail("更新失败");
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/list")
    public RestResult<List<BridgeConfigTO>> list() throws IOException {
        List<PluginConfigDO> doList = pluginConfigMapper.selectAll();
        return RestResult.ok(BridgeConvert.convert(doList));
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/test")
    public RestResult<String> test(BridgeConfigTO config) throws IOException, ExecutionException, InterruptedException {
        HttpResponse response = client.post(OpenApi.BASE_API + "/bridge/" + config.getType() + "/test").postJson(config.getConfig()).submit().get();
        if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return RestResult.fail("该插件服务不存在或未启用");
        } else if (response.statusCode() != 200) {
            LOGGER.error("status:{} reason:{}", response.statusCode(), response.getReasonPhrase());
            return RestResult.fail("test fail");
        }
        RestResult result = JSONObject.parseObject(response.body(), RestResult.class);

        return result.isSuccess() ? RestResult.ok("测试成功") : RestResult.fail(result.getMessage());
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/start")
    public RestResult<String> start(@Param("id") int id) throws IOException, ExecutionException, InterruptedException {
        PluginConfigDO configDO = getById(id);

        HttpResponse response = client.post(OpenApi.BASE_API + "/bridge/" + configDO.getPluginType() + "/start").postJson(getConfigBytes(configDO)).submit().get();
        if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return RestResult.fail("该插件服务不存在或未启用");
        } else if (response.statusCode() != 200) {
            LOGGER.error("status:{} reason:{}", response.statusCode(), response.getReasonPhrase());
            return RestResult.fail("启动失败");
        }
        JSONObject jsonObject = JSONObject.parseObject(response.body());
        if (jsonObject.getInteger("code") != 200) {
            return RestResult.fail(jsonObject.getString("message"));
        }
        pluginConfigMapper.updateStatusById(id, 1);
        return RestResult.ok("启动成功");
    }

    @RequestMapping(OpenApi.BASE_API + "/bridge/stop")
    public RestResult<String> stop(@Param("id") int id) throws Exception {
        PluginConfigDO configDO = getById(id);
        HttpResponse body = client.post(OpenApi.BASE_API + "/bridge/" + configDO.getPluginType() + "/stop").postJson(getConfigBytes(configDO)).submit().get();
        if (body.statusCode() != 200) {
            LOGGER.error("status:{} reason:{}", body.statusCode(), body.getReasonPhrase());
            return RestResult.fail("停止插件失败");
        }
        JSONObject jsonObject = JSONObject.parseObject(body.body());
        if (jsonObject.getInteger("code") != 200) {
            return RestResult.fail(jsonObject.getString("message"));
        }
        pluginConfigMapper.updateStatusById(id, 0);
        return RestResult.ok("停止成功");
    }

    private PluginConfigDO getById(int id) throws IOException {
        PluginConfigDO configDO;
        configDO = pluginConfigMapper.selectById(id);
        ValidateUtils.isTrue(configDO != null, "插件配置不存在");
        return configDO;
    }

    private JSONObject getConfigBytes(PluginConfigDO configDO) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", configDO.getId());
        jsonObject.put("config", JSONObject.parseObject(configDO.getConfig()));
        return jsonObject;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void setPluginConfigMapper(PluginConfigMapper pluginConfigMapper) {
        this.pluginConfigMapper = pluginConfigMapper;
    }
}
