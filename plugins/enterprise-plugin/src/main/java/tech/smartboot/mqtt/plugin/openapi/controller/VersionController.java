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

import com.alibaba.fastjson2.JSONArray;
import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.VersionTO;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Controller
public class VersionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionController.class);
    private VersionTO version;

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        getVersion();
        HashedWheelTimer.DEFAULT_TIMER.schedule(new AsyncTask() {
            @Override
            public void execute() {
                try {
                    getVersion();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1, TimeUnit.HOURS);

    }

    private void getVersion() {
        try {
            HttpClient client = new HttpClient("https://gitee.com/api/v5/repos/smartboot/smart-mqtt/releases");
            HttpResponse response = client.get().
                    addQueryParam("access_token", "be94a321e12d6be8a7e655d9ca2bbf85")
                    .addQueryParam("per_page", "1")
                    .addQueryParam("direction", "desc")
                    .onFailure(Throwable::printStackTrace).submit().get();
            LOGGER.info("response:{}", response.body());
            JSONArray array = JSONArray.parseArray(response.body());
            version = array.getJSONObject(0).to(VersionTO.class);
        } catch (Throwable throwable) {
            LOGGER.warn("get latest version fail:{}", throwable.getMessage());
        }
    }

    @RequestMapping(OpenApi.SYSTEM_VERSION)
    public RestResult<VersionTO> getLatestVersion() {
        if (version == null) {
            version = new VersionTO();
            version.setTagName(version.getCurrent());
        }
        return RestResult.ok(version);
    }

    public static void main(String[] args) {
        HttpClient client = new HttpClient("https://gitee.com/api/v5/repos/smartboot/smart-mqtt/releases");
        client.get().addQueryParam("access_token", "be94a321e12d6be8a7e655d9ca2bbf85").addQueryParam("per_page", "1").addQueryParam("direction", "desc").onSuccess(new Consumer<HttpResponse>() {
            @Override
            public void accept(HttpResponse httpResponse) {
                JSONArray array = JSONArray.parseArray(httpResponse.body());
                VersionTO versionTO = array.getJSONObject(0).to(VersionTO.class);
                System.out.println(versionTO);
            }
        }).onFailure(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
            }
        }).submit();
    }
}
