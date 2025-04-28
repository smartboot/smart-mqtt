/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.spec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public abstract class Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugin.class);
    public static final String CONFIG_FILE_NAME = "plugin.yaml";

    private File storage;
    /**
     * 是否已安装
     */
    private boolean installed;

    /**
     * 启动异常
     */
    private Throwable throwable;

    /**
     * 获取插件名称
     *
     * @return
     */
    public String pluginName() {
        return this.getClass().getSimpleName();
    }


    /**
     * 安装插件,需要在servlet服务启动前调用
     */
    public final void install(BrokerContext brokerContext) throws Throwable {
        checkSate();
        try {
            initPlugin(brokerContext);
            installed = true;
        } catch (Throwable e) {
            throwable = e;
            throw e;
        }
    }

    public boolean isInstalled() {
        return installed;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 初始化插件
     */
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        LOGGER.info("plugin:[" + pluginName() + "] do nothing when initPlugin!");
    }

    /**
     * 卸载插件,在容器服务停止前调用
     */
    public final void uninstall() {
        destroyPlugin();
    }

    /**
     * 销毁插件
     */
    protected void destroyPlugin() {
        LOGGER.info("plugin:[" + pluginName() + "] do nothing when destroyPlugin!");
    }

    private void checkSate() {
        if (installed) {
            throw new IllegalStateException("plugin [ " + pluginName() + " ] has installed!");
        }
    }

    /**
     * 插件排序值，数值小的被优先加载
     */
    public int order() {
        return 0;
    }

    public File storage() {
        if (storage == null) {
            throw new IllegalStateException("plugin [ " + pluginName() + " ] has no store directory!");
        }
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    public abstract String getVersion();

    public abstract String getVendor();


    public abstract String getDescription();

    public int id() {
        // 插件id为类名的hash值
        int hashCode = pluginName().hashCode();
        return hashCode > 0 ? hashCode : -hashCode;
    }

    /**
     * 获取插件配置内容,plugin.yaml
     */
    public String config() {
        File file = new File(storage(), "plugin.yaml");
        if (!file.exists()) {
            return null;
        }
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toString("UTF-8");
        } catch (Exception e) {
            LOGGER.error("get plugin config error", e);
            return null;
        }
    }
}
