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
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.OpenApiConfig;
import tech.smartboot.mqtt.plugin.openapi.to.PluginItem;
import tech.smartboot.mqtt.plugin.openapi.to.RepositoryPlugin;
import tech.smartboot.mqtt.plugin.spec.Plugin;

import java.io.File;
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

/**
 * @author 三刀
 * @version v1.0 4/21/25
 */
@Controller(async = true, value = OpenApi.BASE_API + "/plugin")
public class PluginManagerController {
    private static final Logger logger = LoggerFactory.getLogger(PluginManagerController.class);
    @Autowired
    private File storage;

    @Autowired
    private OpenApiConfig openApiConfig;

    private final Map<Integer, Plugin> enabledPlugins = new HashMap<>();
    private final Map<Integer, List<Plugin>> plugins = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        // 加载已启用的插件
        File baseDir = storage.getParentFile().getParentFile();
        for (File file : baseDir.listFiles()) {
            if (file.isDirectory() || !file.getName().endsWith(".jar")) {
                continue;
            }
            Plugin plugin = loadPlugin(file.toPath());
            if (plugin != null) {
                enabledPlugins.put(plugin.id(), plugin);
            }
        }
        // 加载已安装的插件
        File repository = new File(storage, RepositoryPlugin.REPOSITORY);
        if (repository.isDirectory()) {
            Files.walk(repository.toPath()).filter(path -> path.getFileName().toString().equals(RepositoryPlugin.REPOSITORY_PLUGIN_NAME)).forEach(path -> {
                Plugin p = loadPlugin(path);
                if (p != null) {
                    plugins.computeIfAbsent(p.id(), k -> new ArrayList<>()).add(p);
                }
            });
        }
    }

    private Plugin loadPlugin(Path jarPath) {
        Plugin p = null;
        try {
            ClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, PluginManagerController.class.getClassLoader().getParent());
            ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
            List<Plugin> plugins = new ArrayList<>();
            for (Plugin plugin : serviceLoader) {
                if (plugin.getClass().getClassLoader() == classLoader) {
                    plugins.add(plugin);
                }
            }
            //自动清理无效插件
            if (plugins.size() != 1) {
                logger.warn("invalid plugin: " + jarPath + ",clean it");
                Files.delete(jarPath);
            } else {
                p = plugins.get(0);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return p;
    }

    @RequestMapping("/market")
    public AsyncResponse market() {
        AsyncResponse asyncResponse = new AsyncResponse();
        Feat.httpClient(openApiConfig.getRegistry(), opt -> {
            opt.debug(true);
        }).get("/repository/").onSuccess(resp -> {
            JSONObject jsonObject = JSONObject.parseObject(resp.body()).getJSONObject("data");
            List<PluginItem> result = jsonObject.getList("plugins", PluginItem.class);
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
        plugins.forEach((id, pluginList) -> {
            Plugin plugin = pluginList.get(0);
            PluginItem item = new PluginItem();
            item.setId(plugin.id());
            item.setName(plugin.pluginName());
            item.setAuthor(plugin.getVendor());
            item.setDescription(plugin.getDescription());
            if (enabledPlugins.containsKey(plugin.id())) {
                item.setVersion(enabledPlugins.get(plugin.id()).getVersion());
                item.setStatus("enabled");
            } else {
                item.setVersion(plugin.getVersion());
                item.setStatus("disabled");
            }
            pluginItems.add(item);
        });
        return RestResult.ok(pluginItems);
    }


    @RequestMapping("/download")
    public AsyncResponse download(@Param("plugin") String plugin, @Param("version") String version) throws IOException {
        ValidateUtils.notBlank(openApiConfig.getRegistry(), "registry is empty");
        ValidateUtils.notBlank(plugin, "plugin is empty");
        ValidateUtils.notBlank(version, "version is empty");
        System.out.println("install: " + plugin);
        AsyncResponse response = new AsyncResponse();
        File file = File.createTempFile("smart-mqtt", plugin + ".temp");
        file.deleteOnExit();
        response.getFuture().whenComplete((result, throwable) -> file.delete());
        logger.info("store plugin in " + file.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(file);

        Feat.httpClient(openApiConfig.getRegistry(), opt -> {
            opt.debug(true);
        }).get("/repository/" + plugin + "/" + version + "/download").onResponseBody((response1, bytes, end) -> {
            if (response1.statusCode() == 200) {
                fos.write(bytes);
            }
        }).onSuccess(rsp -> {
            if (rsp.statusCode() != 200) {
                response.complete(RestResult.fail("下载插件失败,httpCode:" + rsp.statusCode() + " statusCode:" + rsp.getReasonPhrase()));
            } else {
                //下载成功，触发安装
                logger.info("下载插件成功");
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    RestResult<String> result = installPlugin(file);
                    response.complete(result);
                } catch (Exception e) {
                    response.complete(RestResult.fail("安装插件失败：" + e.getMessage()));
                }


            }
        }).onFailure(resp -> {
            logger.error("下载插件失", resp);
            response.complete(RestResult.fail("下载插件失败：" + resp.getMessage()));
        }).submit();
        return response;
    }

    @RequestMapping("/uninstall")
    public RestResult<Void> uninstall(@Param("id") int id) throws IOException {
        if (enabledPlugins.containsKey(id)) {
            return RestResult.fail("请先停用该插件");
        }
        List<Plugin> plugins = this.plugins.remove(id);
        if (CollectionUtils.isEmpty(plugins)) {
            return RestResult.fail("该插件不存在");
        }
        Plugin plugin = plugins.get(0);
        File file = new File(storage, "repository/" + plugin.id());
        Files.walk(file.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return RestResult.ok(null);
    }

    @RequestMapping("/enable")
    public RestResult<Void> enable(@Param("id") int id) throws IOException {
        if (enabledPlugins.containsKey(id)) {
            return RestResult.fail("该插件已启用");
        }
        Plugin plugin = plugins.get(id).get(0);
        Path path = Paths.get(storage.getAbsolutePath(), RepositoryPlugin.REPOSITORY, String.valueOf(plugin.id()), plugin.getVersion(), RepositoryPlugin.REPOSITORY_PLUGIN_NAME);
        if (!Files.exists(path)) {
            return RestResult.fail("该插件不存在");
        }
        Files.copy(path, new File(storage.getParentFile().getParentFile(), plugin.pluginName() + "-" + plugin.getVersion() + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        enabledPlugins.put(id, plugin);
        return RestResult.ok(null);
    }

    @RequestMapping("/disable")
    public RestResult<Void> disable(@Param("id") int id) throws IOException {
        Plugin plugin = enabledPlugins.remove(id);
        if (plugin == null) {
            return RestResult.fail("该插件已停用");
        }
        File file = new File(storage.getParentFile().getParentFile(), plugin.pluginName() + "-" + plugin.getVersion() + ".jar");
        if (file.exists()) {
            file.delete();
        }
        return RestResult.ok(null);
    }

    @RequestMapping("/upload")
    public RestResult<String> upload(HttpRequest request) throws IOException, ClassNotFoundException {
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

    private RestResult<String> installPlugin(File tempFile) {
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
        File localRepository = new File(localRepositoryDir, "/plugin.jar");
        if (localRepository.isFile()) {
            return RestResult.fail("本地仓库已存在");
        }
        try {
            Files.copy(tempFile.toPath(), localRepository.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.plugins.computeIfAbsent(plugin.id(), k -> new ArrayList<>()).add(plugin);
        } catch (IOException e) {
            logger.error("插件存储本地仓库失败", e);
            return RestResult.fail("插件存储本地仓库失败");
        }

        //启动插件
        File destFile = new File(storage.getParentFile().getParentFile(), plugin.pluginName() + "-" + plugin.getVersion() + ".jar");
        if (destFile.exists()) {
            return RestResult.fail("插件已存在");
        }
        try {
            Files.copy(tempFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            enabledPlugins.put(plugin.id(), plugin);
        } catch (IOException e) {
            logger.error("安装失败", e);
            return RestResult.fail("插件安装失败");
        }
        return RestResult.ok("插件安装成功");
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    public void setOpenApiConfig(OpenApiConfig openApiConfig) {
        this.openApiConfig = openApiConfig;
    }
}
