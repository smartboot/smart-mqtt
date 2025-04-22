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
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.AsyncResponse;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.PluginItem;
import tech.smartboot.mqtt.plugin.openapi.to.PluginMarket;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author 三刀
 * @version v1.0 4/21/25
 */
@Controller(async = true, value = OpenApi.BASE_API + "/plugin")
public class PluginManagerController {
    private static final Logger logger = LoggerFactory.getLogger(PluginManagerController.class);
    @Autowired
    private File storage;

    @RequestMapping("/market")
    public RestResult<List<PluginItem>> market() {
        Yaml yaml = new Yaml();
        Object object = yaml.load(PluginManagerController.class.getClassLoader().getResourceAsStream("market.yaml"));
        String configJson = JSONObject.toJSONString(object);
        PluginMarket market = JSONObject.parseObject(configJson, PluginMarket.class);
        return RestResult.ok(market.getPlugins());
    }


    @RequestMapping("/download")
    public AsyncResponse download(@Param("id") String id) throws IOException {
        System.out.println("install: " + id);
        AsyncResponse response = new AsyncResponse();
        File file = File.createTempFile(id, ".jar");
        logger.info("store plugin in " + file.getAbsolutePath());
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Feat.httpClient("https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png", opt -> {
            }).get().onResponseBody(new Stream() {
                @Override
                public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                    fos.write(bytes);
                }
            }).onSuccess(rsp -> {
                //下载成功，触发安装
                logger.info("下载插件成功");
                response.complete(installPlugin(file));
            }).onFailure(resp -> {
                logger.error("下载插件失", resp);
                response.complete(RestResult.fail("下载插件失败：" + resp.getMessage()));
            }).submit().thenAccept(resp -> file.delete());
        }

        return response;
    }

    @RequestMapping("/uninstall")
    public RestResult<Void> uninstall(@Param("id") String pluginKey) {
        return RestResult.ok(null);
    }

    @RequestMapping("/upload")
    public RestResult<String> upload(HttpRequest request) throws IOException, ClassNotFoundException {
        Part part = request.getPart("plugin");
        if (part == null) {
            return RestResult.fail("plugin is null");
        }


        File tempFile = File.createTempFile(part.getSubmittedFileName(), ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            InputStream inputStream = part.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            inputStream.close();
        }
        try {
            return installPlugin(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private RestResult<String> installPlugin(File tempFile) {
        URL url = null;
        try {
            url = tempFile.toURI().toURL();
        } catch (MalformedURLException e) {
            return RestResult.fail(e.getMessage());
        }
        List<Plugin> plugins = new ArrayList<>();
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url});
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, urlClassLoader);
        serviceLoader.forEach(plugin -> {
            if (plugin.getClass().getClassLoader() == urlClassLoader) {
                plugins.add(plugin);
            }
        });
        if (plugins.isEmpty()) {
            return RestResult.fail("无效的插件");
        }
        if (plugins.size() > 1) {
            return RestResult.fail("不支持混合插件");
        }
        Plugin plugin = plugins.get(0);
        File destFile = new File(storage.getParentFile().getParentFile(), plugin.pluginName() + plugin.getVersion() + ".jar");
        if (destFile.exists()) {
            return RestResult.fail("插件已存在");
        }
        return tempFile.renameTo(destFile) ? RestResult.ok("安装成功") : RestResult.fail("安装失败");
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }
}
