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

import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author 三刀
 * @version v1.0 4/14/25
 */
public class PluginContainer extends Plugin {
    private final List<Plugin> plugins = new ArrayList<>();

    public PluginContainer(ClassLoader classLoader) {
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
        for (Plugin plugin : serviceLoader) {
            plugins.add(plugin);
        }
    }

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        for (Plugin plugin : plugins) {
            plugin.install(brokerContext);
        }
    }

    @Override
    protected void destroyPlugin() {
        for (Plugin plugin : plugins) {
            plugin.uninstall();
        }
    }
}
