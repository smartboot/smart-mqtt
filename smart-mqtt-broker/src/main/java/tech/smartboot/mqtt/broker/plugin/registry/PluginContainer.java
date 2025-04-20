/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.plugin.registry;

import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author 三刀
 * @version v1.0 4/14/25
 */
public class PluginContainer extends Plugin {
    private final List<Plugin> plugins = new ArrayList<>();
    private final ClassLoader classLoader;
    private File storage;

    public PluginContainer(ClassLoader classLoader, File storage) {
        this.classLoader = classLoader;
        this.storage = storage;
    }

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        ClassLoader preClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
        System.out.println("Plugin container loaded!" + serviceLoader);
        for (Plugin plugin : serviceLoader) {
            if (plugin.getClass().getClassLoader() == classLoader) {
                plugins.add(plugin);
                plugin.setStorage(storage);
                plugin.install(brokerContext);
            }
        }

        Thread.currentThread().setContextClassLoader(preClassLoader);
    }

    @Override
    protected void destroyPlugin() {
        ClassLoader preClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        for (Plugin plugin : plugins) {

            plugin.uninstall();
        }
        Thread.currentThread().setContextClassLoader(preClassLoader);
    }
}
