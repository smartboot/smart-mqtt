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

import tech.smartboot.feat.cloud.AsyncResponse;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.mqtt.plugin.openapi.to.PluginTO;
import tech.smartboot.mqtt.plugin.openapi.to.RepositoryPlugin;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 4/22/25
 */
@Controller("/repository")
public class PluginRepositoryController {
    private static final Logger logger = LoggerFactory.getLogger(PluginRepositoryController.class);
    @Autowired
    private File storage;

    @RequestMapping("/")
    public RestResult<RepositoryPlugin> list() throws FileNotFoundException, MalformedURLException {
        RepositoryPlugin pluginMarket = new RepositoryPlugin();
        pluginMarket.setPlugins(new ArrayList<>());

        File file = new File(storage, RepositoryPlugin.REPOSITORY);
        if (!file.isDirectory()) {
            logger.error("repository directory not exists");
            return RestResult.ok(pluginMarket);
        }
        for (File pluginFile : Objects.requireNonNull(file.listFiles((dir, name) -> name.endsWith(".jar")))) {
            PluginTO item = new PluginTO();

            URLClassLoader classLoader = new URLClassLoader(new URL[]{pluginFile.toURI().toURL()}, PluginRepositoryController.class.getClassLoader());
            ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
            for (Plugin plugin : serviceLoader) {
                if (plugin.getClass().getClassLoader() == classLoader) {
                    item.setId(plugin.id());
                    item.setName(plugin.pluginName());
                    item.setVendor(plugin.getVendor());
                    item.setVersion(plugin.getVersion());
                    item.setDescription(plugin.getDescription());
                    item.setSize(pluginFile.length());
                    item.setUrl("/repository/" + pluginFile.getName().substring(0, pluginFile.getName().length() - 4) + "/download");
                    break;
                }
            }
            if (FeatUtils.isNotBlank(item.getName())) {
                pluginMarket.getPlugins().add(item);
            }
        }
        return RestResult.ok(pluginMarket);
    }

    @RequestMapping("/:plugin/download")
    public AsyncResponse download(@PathParam(value = "plugin") String pluginName, HttpResponse response) throws FileNotFoundException {
        AsyncResponse asyncResponse = new AsyncResponse();
        File file = new File(storage, "repository/" + pluginName + ".jar");
        if (!file.isFile()) {
            response.setHttpStatus(HttpStatus.NOT_FOUND);
            asyncResponse.complete();
            return asyncResponse;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + pluginName + ".jar");
        response.setContentLength(file.length());
        if (file.length() == 0) {
            response.setHttpStatus(HttpStatus.OK);
            asyncResponse.complete();
            return asyncResponse;
        }
        FileInputStream fis = new FileInputStream(file);
        ByteBuffer buffer = ByteBuffer.allocate(4094);
        buffer.position(buffer.limit());
        Consumer<FeatOutputStream> consumer = new Consumer<FeatOutputStream>() {
            final AtomicLong readPos = new AtomicLong(0);

            @Override
            public void accept(FeatOutputStream result) {
                try {
                    buffer.compact();
                    int len = fis.getChannel().read(buffer);
                    buffer.flip();
                    if (len == -1) {
                        asyncResponse.getFuture().completeExceptionally(new IOException("EOF"));
                    } else if (readPos.addAndGet(len) >= response.getContentLength()) {
                        response.getOutputStream().transferFrom(buffer, bufferOutputStream -> asyncResponse.complete(null));
                    } else {
                        response.getOutputStream().transferFrom(buffer, this);
                    }
                } catch (Throwable throwable) {
                    asyncResponse.getFuture().completeExceptionally(throwable);
                }
            }
        };
        consumer.accept(response.getOutputStream());
        return asyncResponse;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }
}
