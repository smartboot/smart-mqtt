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

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.PluginException;
import tech.smartboot.mqtt.plugin.spec.PluginRegistry;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version v1.0 4/25/25
 */
class PluginRegistryImpl implements PluginRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRegistry.class);
    private final Map<Integer, PluginUnit> plugins = new ConcurrentHashMap<>();
    private final BrokerContext brokerContext;
    private File baseDir;

    public PluginRegistryImpl(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void init() throws Throwable {
        String baseDirPath = System.getProperty("SMART_MQTT_PLUGINS");
        if (baseDirPath == null) {
            baseDirPath = System.getenv("SMART_MQTT_PLUGINS");
        }
        if (baseDirPath == null) {
            LOGGER.warn("SMART_MQTT_PLUGINS is not set,plugin will not be loaded!");
            return;
        }
        baseDir = new File(baseDirPath);
        if (baseDir.isFile()) {
            throw new PluginException("SMART_MQTT_PLUGINS is a file,plugin will not be loaded!");
        }
        if (!baseDir.isDirectory()) {
            baseDir.mkdirs();
        }
        File baseStorage = new File(baseDir, "_storage");
        if (baseStorage.isFile()) {
            throw new PluginException("storage is a file,plugin will not be loaded!");
        }
        if (!baseStorage.isDirectory()) {
            baseStorage.mkdirs();
        }
        for (File file : Objects.requireNonNull(baseDir.listFiles())) {
            if (!file.getName().endsWith(".jar")) {
                continue;
            }
            URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, PluginRegistry.class.getClassLoader());
            PluginContainer pluginContainer = new PluginContainer(classLoader, baseStorage);
            LOGGER.info("registryPlugin load plugin:{}", pluginContainer.pluginName());
            // 插件目录下可能存在无效的插件
            try {
                plugins.put(pluginContainer.id(), new PluginUnit(pluginContainer, file));
                pluginContainer.install(brokerContext);
            } catch (Throwable e) {
                LOGGER.error("registryPlugin install plugin:{} exception", file.getName(), e);
            }
        }

        dumpPluginList();
    }

    private void dumpPluginList() {
        // 计算最长插件名长度
        int maxNameLength = plugins.values().stream().mapToInt(p -> p.plugin.pluginName().length()).max().orElse(16);
        maxNameLength = Math.max(maxNameLength, 16); // 最小保持16字符宽度

        // 动态生成表格边框
        String topBorder = "╔" + repeat("═", maxNameLength + 2) + "╤════════════╗";
        String headerBorder = "╠" + repeat("═", maxNameLength + 2) + "╪════════════╣";
        String bottomBorder = "╚" + repeat("═", maxNameLength + 2) + "╧════════════╝";

        System.out.println(topBorder);
        System.out.printf("║ %-" + maxNameLength + "s │ Status     ║%n", "Plugin Name");
        System.out.println(headerBorder);

        int finalMaxNameLength = maxNameLength;
        plugins.values().stream().sorted(Comparator.comparingInt(o -> o.plugin.id())).forEach(pluginUnit -> {
            String status;
            if (pluginUnit.plugin.isInstalled()) {
                status = "\033[32m✓ success\033[0m"; // 绿色
            } else {
                status = "\033[31m✗ fail   \033[0m";    // 红色
            }
            System.out.printf("║ %-" + finalMaxNameLength + "s │ %-10s  ║%n", pluginUnit.plugin.pluginName(), status);
        });
        System.out.println(bottomBorder);
    }

    private String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public void stopPlugin(int pluginId) {
        PluginUnit plugin = plugins.remove(pluginId);
        if (plugin != null) {
            LOGGER.info("registryPlugin stop plugin:{} version:{}", plugin.plugin.pluginName(), plugin.plugin.getVersion());
            plugin.plugin.uninstall();
        }
    }

    @Override
    public boolean containsPlugin(int pluginId) {
        return plugins.containsKey(pluginId);
    }

    @Override
    public Plugin getPlugin(int pluginId) {
        PluginUnit plugin = plugins.get(pluginId);
        if (plugin == null) {
            return null;
        }
        return plugin.plugin;
    }

    public void startPlugin(int pluginId) {
        if (containsPlugin(pluginId)) {
            throw new PluginException("This plugin is already running.");
        }
        File baseStorage = new File(baseDir, "_storage");
        for (File file : Objects.requireNonNull(baseDir.listFiles())) {
            if (!file.getName().endsWith(".jar")) {
                continue;
            }
            if (plugins.values().stream().anyMatch(pluginUnit -> pluginUnit.pluginFile.getAbsoluteFile().equals(file.getAbsoluteFile()))) {
                continue;
            }
            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, PluginRegistry.class.getClassLoader());
                PluginContainer pluginContainer = new PluginContainer(classLoader, baseStorage);
                if (pluginContainer.id() != pluginId) {
                    continue;
                }
                LOGGER.info("registryPlugin load plugin:{}", pluginContainer.pluginName());
                // 插件目录下可能存在无效的插件
                plugins.put(pluginContainer.id(), new PluginUnit(pluginContainer, file));
                pluginContainer.install(brokerContext);
                LOGGER.info("registryPlugin start plugin:{}, version:{}", pluginContainer.pluginName(), pluginContainer.getVersion());
            } catch (ClassNotFoundException e) {
                LOGGER.error("registryPlugin install error:{}, check ", e.getMessage());
            } catch (Throwable e) {
                LOGGER.error("registryPlugin install plugin:{} exception", file.getName(), e);
            }
        }
    }

    public void destroy() {
        plugins.forEach((id, plugin) -> {
            plugin.plugin.uninstall();
        });
        plugins.clear();
    }

    static class PluginUnit {
        Plugin plugin;
        File pluginFile;

        public PluginUnit(Plugin plugin, File pluginFile) {
            this.plugin = plugin;
            this.pluginFile = pluginFile;
        }
    }

}
