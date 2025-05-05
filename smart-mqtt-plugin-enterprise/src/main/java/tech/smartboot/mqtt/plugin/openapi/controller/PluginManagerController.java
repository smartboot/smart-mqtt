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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.AsyncResponse;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.common.utils.NumberUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.enums.PluginStatusEnum;
import tech.smartboot.mqtt.plugin.openapi.to.PluginItem;
import tech.smartboot.mqtt.plugin.openapi.to.RepositoryPlugin;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀
 * @version v1.0 4/21/25
 */
@Controller(value = OpenApi.BASE_API + "/plugin")
public class PluginManagerController {
    private static final Logger logger = LoggerFactory.getLogger(PluginManagerController.class);
    @Autowired
    private File storage;

    @Autowired
    private PluginConfig pluginConfig;

    @Autowired
    private BrokerContext brokerContext;

    private final Map<Integer, List<Plugin>> localPlugins = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        // 加载已安装的插件
        File repository = new File(storage, RepositoryPlugin.REPOSITORY);
        if (repository.isDirectory()) {
            Files.walk(repository.toPath()).filter(path -> path.getFileName().toString().equals(RepositoryPlugin.REPOSITORY_PLUGIN_NAME)).forEach(path -> {
                loadPlugin(path);
            });
        }
    }

    private void loadPlugin(Path jarPath) {
        try {
            ClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, PluginManagerController.class.getClassLoader().getParent());
            ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
            List<Plugin> plugins = new ArrayList<>();
            for (Plugin plugin : serviceLoader) {
                if (plugin.getClass().getClassLoader() == classLoader) {
                    File pluginStorage = new File(storage.getParent(), String.valueOf(plugin.id()));
                    if (!pluginStorage.exists()) {
                        pluginStorage.mkdirs();
                    }
                    plugin.setStorage(pluginStorage);
                    plugins.add(plugin);
                }
            }
            //自动清理无效插件
            if (plugins.size() != 1) {
                logger.warn("invalid plugin: " + jarPath + ",clean it");
                Files.delete(jarPath);
            } else {
                Plugin p = plugins.get(0);
                List<Plugin> list = localPlugins.computeIfAbsent(p.id(), k -> new ArrayList<>());
                list.removeIf(plugin -> plugin.getVersion().equals(p.getVersion()));
                list.add(p);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/:id/config")
    public RestResult<String> config(@PathParam("id") String id) {
        List<Plugin> plugins = localPlugins.get(NumberUtils.toInt(id, 0));
        if (CollectionUtils.isEmpty(plugins)) {
            return RestResult.fail("插件不存在");
        }
        Plugin plugin = plugins.get(0);
        File file = new File(plugin.storage(), "plugin.yaml");
        if (!file.exists()) {
            return RestResult.ok("");
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return RestResult.ok(FeatUtils.asString(inputStream));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return RestResult.fail(e.getMessage());
        }
    }

    @RequestMapping("/:id/config/save")
    public RestResult<Void> config(@PathParam("id") String id, @Param("config") String config) throws Throwable {
        if (StringUtils.isBlank(config)) {
            return RestResult.fail("配置内容为空");
        }
        List<Plugin> plugins = localPlugins.get(NumberUtils.toInt(id, 0));
        if (CollectionUtils.isEmpty(plugins)) {
            return RestResult.fail("插件不存在");
        }
        Plugin plugin = plugins.get(0);
        File file = new File(plugin.storage(), "plugin.yaml");
        try (FileOutputStream outputStream = new FileOutputStream(file);) {
            outputStream.write(config.getBytes());
            outputStream.flush();

        } catch (IOException e) {
            return RestResult.fail(e.getMessage());
        }
        if (brokerContext.pluginRegistry().getPlugin(plugin.id()) == null) {
            return RestResult.ok(null);
        }
        //自动重启
        RestResult<Void> result = disable(plugin.id());
        if (!result.isSuccess()) {
            return result;
        }
        return enable(plugin.id());
    }

    @RequestMapping("/market")
    public AsyncResponse market() {
        AsyncResponse asyncResponse = new AsyncResponse();
        if (StringUtils.isBlank(pluginConfig.getRegistry())) {
            asyncResponse.complete(RestResult.fail("registry is empty"));
            return asyncResponse;
        }

        Feat.httpClient(pluginConfig.getRegistry(), opt -> {
            opt.debug(true);
        }).get("/repository/").onSuccess(resp -> {
            JSONObject jsonObject = JSONObject.parseObject(resp.body()).getJSONObject("data");
            List<PluginItem> result = jsonObject.getList("plugins", PluginItem.class);
            result.forEach(pluginItem -> {
                pluginItem.setStatus(PluginStatusEnum.UNINSTALLED.getCode());
                List<Plugin> plugins = localPlugins.get(pluginItem.getId());
                if (CollectionUtils.isNotEmpty(plugins)) {
                    setPluginStatus(plugins.get(0), pluginItem);
                }
            });
            asyncResponse.complete(RestResult.ok(result));
        }).onFailure(error -> {
            asyncResponse.complete(RestResult.fail(error.getMessage()));
        }).submit();
        return asyncResponse;
    }

    /**
     * 获取本地插件列表
     */
    @RequestMapping("/list")
    public RestResult<List<PluginItem>> list() throws IOException {
        List<PluginItem> pluginItems = new ArrayList<>();
        localPlugins.forEach((id, pluginList) -> {
            Plugin plugin = pluginList.get(0);
            PluginItem item = new PluginItem();
            item.setId(plugin.id());
            item.setName(plugin.pluginName());
            item.setDescription(plugin.getDescription());
            item.setVendor(plugin.getVendor());
            setPluginStatus(plugin, item);
            pluginItems.add(item);
        });
        return RestResult.ok(pluginItems);
    }

    private void setPluginStatus(Plugin plugin, PluginItem item) {
        Plugin enabledPlugins = brokerContext.pluginRegistry().getPlugin(plugin.id());
        if (enabledPlugins != null) {
            item.setVersion(enabledPlugins.getVersion());
            if (enabledPlugins.isInstalled()) {
                item.setStatus(PluginStatusEnum.ENABLED.getCode());
            } else if (enabledPlugins.getThrowable() != null) {
                item.setStatus(PluginStatusEnum.ERROR.getCode());
                item.setMessage(enabledPlugins.getThrowable().toString());
            } else {
                item.setStatus(PluginStatusEnum.ERROR.getCode());
                item.setMessage(PluginStatusEnum.ERROR.getDesc());
            }
        } else {
            item.setVersion(plugin.getVersion());
            item.setStatus(PluginStatusEnum.DISABLED.getCode());
        }
    }


    @RequestMapping("/download")
    public void download(@Param("url") String url, HttpRequest request) throws IOException {
        ValidateUtils.notBlank(pluginConfig.getRegistry(), "registry is empty");
        ValidateUtils.notBlank(url, "插件下载地址未知");
        File file = File.createTempFile("smart-mqtt", url.hashCode() + ".temp");
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);

        //连接远程仓库
        HttpClient httpClient = Feat.httpClient(pluginConfig.getRegistry(), opt -> {
//            opt.debug(true);
        });
        AtomicLong fileSize = new AtomicLong();
        AtomicLong downloadSize = new AtomicLong();

        request.upgrade(new SSEUpgrade() {
            @Override
            public void onOpen(SseEmitter sseEmitter) throws IOException {
                logger.info("store plugin in " + file.getAbsolutePath());
                sseEmitter.sendAsJson(RestResult.ok(0));

                httpClient.get(url)
                        //获取文件大小
                        .onResponseHeader(httpResponse -> {
                            if (httpResponse.statusCode() != 200) {
                                sseEmitter.sendAsJson(RestResult.fail("下载插件失败,httpCode:" + httpResponse.statusCode() + " statusCode:" + httpResponse.getReasonPhrase()));
                                sseEmitter.complete();
                            } else {
                                fileSize.set(httpResponse.getContentLength());
                            }
                        })
                        //下载文件,推送进度
                        .onResponseBody((response1, bytes, end) -> {
                            if (response1.statusCode() == 200) {
                                fos.write(bytes);
                                //使其不超过100%
                                if (bytes.length > 0) {
                                    sseEmitter.sendAsJson(RestResult.ok((downloadSize.getAndAdd(bytes.length) * 100 / fileSize.get())));
                                }
                            }
                        })
                        //下载完成,触发安装
                        .onSuccess(rsp -> {
                            if (rsp.statusCode() == 200) {
                                //下载成功，触发安装
                                logger.info("下载插件成功");
                                try {
                                    fos.flush();
                                    fos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    RestResult<Void> result = installPlugin(file);
                                    if (result.isSuccess()) {
                                        sseEmitter.sendAsJson(RestResult.ok(100));
                                    } else {
                                        sseEmitter.sendAsJson(result);
                                    }
                                } catch (Throwable e) {
                                    sseEmitter.sendAsJson(RestResult.fail("安装插件失败：" + e.getMessage()));
                                }
                            }
                            sseEmitter.complete();
                        })
                        //下载失败
                        .onFailure(resp -> {
                            logger.error("下载插件失", resp);
                            sseEmitter.sendAsJson(RestResult.fail("下载插件失败：" + resp.getMessage()));
                            sseEmitter.complete();
                        }).submit();
            }

            @Override
            public void destroy() {
                super.destroy();
                file.delete();
                httpClient.close();
            }
        });
    }

    @RequestMapping("/uninstall")
    public RestResult<Void> uninstall(@Param("id") int id) throws IOException {
        if (brokerContext.pluginRegistry().containsPlugin(id)) {
            return RestResult.fail("请先停用该插件");
        }
        List<Plugin> plugins = this.localPlugins.remove(id);
        if (CollectionUtils.isEmpty(plugins)) {
            return RestResult.fail("该插件不存在");
        }
        Plugin plugin = plugins.get(0);
        File file = new File(storage, "repository/" + plugin.id());
        Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        Files.walk(plugin.storage().toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        return RestResult.ok(null);
    }

    @RequestMapping("/enable")
    public RestResult<Void> enable(@Param("id") int id) throws Throwable {
        if (brokerContext.pluginRegistry().containsPlugin(id)) {
            return RestResult.fail("该插件已启用");
        }
        Plugin plugin = localPlugins.get(id).get(0);
        Path path = Paths.get(storage.getAbsolutePath(), RepositoryPlugin.REPOSITORY, String.valueOf(plugin.id()), plugin.getVersion(), RepositoryPlugin.REPOSITORY_PLUGIN_NAME);
        if (!Files.exists(path)) {
            return RestResult.fail("该插件不存在");
        }
        Files.copy(path, new File(storage.getParentFile().getParentFile(), plugin.id() + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        brokerContext.pluginRegistry().startPlugin(plugin.id());
        return RestResult.ok(null);
    }

    @RequestMapping("/disable")
    public RestResult<Void> disable(@Param("id") int id) {
        List<Plugin> p = localPlugins.get(id);
        if (CollectionUtils.isEmpty(p)) {
            return RestResult.fail("无法停用非本地仓库插件");
        }
        Plugin plugin = p.get(0);
        File file = new File(storage.getParentFile().getParentFile(), plugin.id() + ".jar");
        if (file.exists() && !file.delete()) {
            return RestResult.fail("插件停用失败!");
        }
        brokerContext.pluginRegistry().stopPlugin(id);
        return RestResult.ok(null);
    }

    @RequestMapping("/upload")
    public RestResult<Void> upload(HttpRequest request) throws Throwable {
        Part part = request.getPart("plugin");
        if (part == null) {
            return RestResult.fail("plugin is null");
        }


        File tempFile = File.createTempFile(part.getSubmittedFileName(), ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            InputStream inputStream = part.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            inputStream.close();
        }
        try {
            return installPlugin(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private RestResult<Void> installPlugin(File tempFile) throws Throwable {
        URL url = null;
        try {
            url = tempFile.toURI().toURL();
        } catch (MalformedURLException e) {
            return RestResult.fail(e.getMessage());
        }
        List<Plugin> plugins = new ArrayList<>();
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url}, PluginManagerController.class.getClassLoader().getParent());
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, urlClassLoader);
        serviceLoader.forEach(plugin -> {
            if (plugin.getClass().getClassLoader() == urlClassLoader) {
                plugins.add(plugin);
            }
        });
        if (plugins.isEmpty()) {
            return RestResult.fail("无效的插件");
        }
        if (plugins.size() > 1) {
            return RestResult.fail("不支持混合插件");
        }
        Plugin plugin = plugins.get(0);
        //部署到本地仓库
        File localRepositoryDir = new File(storage, "repository/" + plugin.id() + "/" + plugin.getVersion());
        if (!localRepositoryDir.exists()) {
            localRepositoryDir.mkdirs();
        }
        File localRepository = new File(localRepositoryDir, "plugin.jar");
        try {
            Files.copy(tempFile.toPath(), localRepository.toPath(), StandardCopyOption.REPLACE_EXISTING);
            loadPlugin(localRepository.toPath());
        } catch (IOException e) {
            logger.error("插件存储本地仓库失败", e);
            return RestResult.fail("插件存储本地仓库失败");
        }
        //当前插件正在使用中，自动启用
        if (brokerContext.pluginRegistry().getPlugin(plugin.id()) != null) {
            disable(plugin.id());
            return enable(plugin.id());
        } else {
            return RestResult.ok(null);
        }

    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }
}
