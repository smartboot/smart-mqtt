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
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 4/22/25
 */
@Controller("/repository")
public class PluginRepositoryController {

    @Autowired
    private File storage;

    @RequestMapping("/:plugin/:version/download")
    public AsyncResponse download(@PathParam(value = "plugin") String pluginName, @PathParam(value = "version") String version, HttpResponse response) throws FileNotFoundException {
        AsyncResponse asyncResponse = new AsyncResponse();
        File file = new File(storage, "repository/" + pluginName + "/" + version + "/" + pluginName + "-" + version + ".jar");
        if (!file.isFile()) {
            response.setHttpStatus(HttpStatus.NOT_FOUND);
            asyncResponse.complete();
            return asyncResponse;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + pluginName + "-" + version + ".jar");
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
