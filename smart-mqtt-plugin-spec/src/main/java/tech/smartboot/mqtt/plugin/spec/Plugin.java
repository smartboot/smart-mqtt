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

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public abstract class Plugin {
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

    private List<PluginPort> usagePorts;

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
        PrintStream out = System.out;
        try {
            System.setOut(new PrintStream(out) {
                @Override
                public void print(String x) {
                    super.print("[" + pluginName() + "] " + x);
                }
            });
            initPlugin(brokerContext);
            installed = true;
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            System.setOut(out);
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
//        LOGGER.info("plugin:[" + pluginName() + "] do nothing when initPlugin!");
    }

    /**
     * 卸载插件,在容器服务停止前调用
     */
    public final void uninstall() {
        PrintStream out = System.out;
        try {
            System.setOut(new PrintStream(out) {
                @Override
                public void print(String x) {
                    super.print("[" + pluginName() + "] " + x);
                }
            });
            destroyPlugin();
        } finally {
            System.setOut(out);
        }
    }

    /**
     * 销毁插件
     */
    protected void destroyPlugin() {
//        LOGGER.info("plugin:[" + pluginName() + "] do nothing when destroyPlugin!");
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


    public String getDescription() {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("readme.md");) {
            if (inputStream == null) {
                return "readme.md not found";
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "readme.md not found";
    }

    public int id() {
        // 插件id为类名的hash值
        int hashCode = pluginName().hashCode();
        return hashCode > 0 ? hashCode : -hashCode;
    }

    public <T> T loadPluginConfig(Class<T> clazz) {
        Constructor constructor = new Constructor(new LoaderOptions());
        constructor.setPropertyUtils(new CamelCasePropertyUtils());
        constructor.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(constructor);
        return yaml.loadAs(config(), clazz);
    }

    /**
     * 添加插件使用端口
     *
     * @param port 使用端口
     * @param desc 描述
     */
    protected void addUsagePort(int port, String desc) {
        if (usagePorts == null) {
            usagePorts = new LinkedList<>();
        }
        usagePorts.add(new PluginPort(port, desc));
    }

    public List<PluginPort> getUsagePorts() {
        if (usagePorts == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(usagePorts);
    }

    private static class CamelCasePropertyUtils extends PropertyUtils {

        @Override
        public Property getProperty(Class<?> type, String name) {
            // 将下划线命名转换为驼峰命名
            String camelCaseName = underlineToCamelCase(name);
            return super.getProperty(type, camelCaseName);
        }

        private String underlineToCamelCase(String name) {
            if (name == null || !name.contains("_")) {
                return name;
            }

            StringBuilder result = new StringBuilder();
            String[] parts = name.split("_");

            // 第一部分保持小写
            result.append(parts[0]);

            // 后续部分首字母大写
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    result.append(Character.toUpperCase(parts[i].charAt(0)))
                            .append(parts[i].substring(1));
                }
            }

            return result.toString();
        }
    }

    /**
     * 获取插件配置内容,plugin.yaml
     */
    private String config() {
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
            e.printStackTrace();
            return null;
        }
    }
}
