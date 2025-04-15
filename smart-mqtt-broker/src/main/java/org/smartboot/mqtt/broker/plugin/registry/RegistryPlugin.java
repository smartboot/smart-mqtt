/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.plugin.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author 三刀
 * @version v1.0 4/14/25
 */
public class RegistryPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPlugin.class);
    private File baseDir;
    private final List<PluginContainer> plugins = new ArrayList<>();

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        String baseDirPath = System.getProperty("SMART_MQTT_PLUGINS");
        if (baseDirPath == null) {
            baseDirPath = System.getenv("SMART_MQTT_PLUGINS");
        }
        if (baseDirPath == null) {
            LOGGER.warn("SMART_MQTT_PLUGINS is not set,plugin will not be loaded!");
            return;
        }
        baseDir = new File(baseDirPath);
        if (!baseDir.isDirectory()) {
            LOGGER.warn("SMART_MQTT_PLUGINS is not a directory,plugin will not be loaded!");
            return;
        }
        for (File file : Objects.requireNonNull(baseDir.listFiles())) {
            if (file.getName().endsWith(".jar")) {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
                plugins.add(new PluginContainer(classLoader));
            }
        }
        for (PluginContainer plugin : plugins) {
            plugin.initPlugin(brokerContext);
        }
    }

    @Override
    protected void destroyPlugin() {
        for (PluginContainer plugin : plugins) {
            plugin.destroyPlugin();
        }
    }
}
