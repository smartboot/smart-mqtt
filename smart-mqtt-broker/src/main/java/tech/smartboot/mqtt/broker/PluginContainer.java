/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker;

import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.PluginException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author 三刀
 * @version v1.0 4/14/25
 */
class PluginContainer extends Plugin {
    private final ClassLoader classLoader;
    private final Plugin plugin;

    public PluginContainer(ClassLoader classLoader, File baseStorage) {
        this.classLoader = classLoader;
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
        System.out.println("Plugin container loaded!" + serviceLoader);
        List<Plugin> plugins = new ArrayList<>();
        serviceLoader.forEach(p -> {
            if (p.getClass().getClassLoader() == classLoader) {
                plugins.add(p);
            }
        });
        if (plugins.size() != 1) {
            throw new RuntimeException("plugin container init error");
        }
        this.plugin = plugins.get(0);
        File storage = new File(baseStorage, String.valueOf(plugin.id()));
        if (!storage.exists()) {
            storage.mkdirs();
        }
        File storageFile = new File(storage, Plugin.CONFIG_FILE_NAME);
        if (!storageFile.exists()) {
            InputStream inputStream = classLoader.getResourceAsStream(Plugin.CONFIG_FILE_NAME);
            if (inputStream != null) {
                try (FileOutputStream outputStream = new FileOutputStream(storageFile);) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    throw new PluginException(e.getMessage(), e);
                }
            }
        }
        this.plugin.setStorage(storage);
    }

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        ClassLoader preClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        plugin.install(brokerContext);
        Thread.currentThread().setContextClassLoader(preClassLoader);
    }

    @Override
    protected void destroyPlugin() {
        ClassLoader preClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        plugin.uninstall();
        Thread.currentThread().setContextClassLoader(preClassLoader);
    }

    @Override
    public String pluginName() {
        return plugin.pluginName();
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public String getVendor() {
        return plugin.getVendor();
    }

    @Override
    public String getDescription() {
        return plugin.getDescription();
    }

    @Override
    public File storage() {
        return plugin.storage();
    }

    @Override
    public void setStorage(File storage) {
        plugin.setStorage(storage);
    }

    @Override
    public int id() {
        return plugin.id();
    }
}
