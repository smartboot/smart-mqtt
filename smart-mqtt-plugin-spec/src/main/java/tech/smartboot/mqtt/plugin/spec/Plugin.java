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

import java.io.File;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public abstract class Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugin.class);

    private File storage;
    /**
     * 是否已安装
     */
    private boolean installed;

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
        initPlugin(brokerContext);
        installed = true;
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

    public final int id() {
        // 插件id为类名的hash值
        int hashCode = pluginName().hashCode();
        return hashCode > 0 ? hashCode : -hashCode;
    }
}
